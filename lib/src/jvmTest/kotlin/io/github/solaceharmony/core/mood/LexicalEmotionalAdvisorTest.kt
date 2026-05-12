package io.github.solaceharmony.core.mood

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LexicalEmotionalAdvisorTest {

    @Test
    fun `analyze returns null on blank input`() = runTest {
        val advisor = LexicalEmotionalAdvisor()
        assertNull(advisor.analyze(""))
        assertNull(advisor.analyze("   \n  "))
    }

    @Test
    fun `analyze returns null when no lexicon match`() = runTest {
        val advisor = LexicalEmotionalAdvisor()
        // Pure noun-phrase with no affective vocabulary — shouldn't trigger.
        assertNull(advisor.analyze("the green pencil sat on the desk"))
    }

    @Test
    fun `frustration is detected`() = runTest {
        val advisor = LexicalEmotionalAdvisor()
        val cue = advisor.analyze("I'm so frustrated, this stupid thing keeps failing again")
        assertNotNull(cue)
        assertEquals(Emotion.FRUSTRATION, cue.emotion)
        assertTrue(cue.confidence >= 0.4f, "confidence should clear threshold, got ${cue.confidence}")
        assertTrue(cue.intensity > 0f, "intensity should be positive, got ${cue.intensity}")
        assertTrue(cue.evidence.isNotEmpty(), "evidence should include matched fragments")
        assertEquals("LexicalEmotionalAdvisor", cue.source)
    }

    @Test
    fun `joy is detected`() = runTest {
        val advisor = LexicalEmotionalAdvisor()
        val cue = advisor.analyze("This is great! I love it, thank you so much!")
        assertNotNull(cue)
        assertEquals(Emotion.JOY, cue.emotion)
    }

    @Test
    fun `sadness is detected`() = runTest {
        val advisor = LexicalEmotionalAdvisor()
        val cue = advisor.analyze("I'm just so tired and lonely lately, everything feels miserable")
        assertNotNull(cue)
        assertEquals(Emotion.SADNESS, cue.emotion)
    }

    @Test
    fun `prompt suggestion accompanies non-neutral non-calm emotions`() = runTest {
        val advisor = LexicalEmotionalAdvisor()
        val cue = advisor.analyze("I'm afraid this won't work, I'm worried about it")
        assertNotNull(cue)
        assertEquals(Emotion.FEAR, cue.emotion)
        assertNotNull(cue.promptSuggestion)
    }

    @Test
    fun `low-confidence input below threshold returns null`() = runTest {
        // High threshold, same lexicon. A genuinely ambiguous mixed signal
        // should fall below the threshold and return silence rather than
        // emit a low-confidence Neutral cue.
        val advisor = LexicalEmotionalAdvisor(confidenceThreshold = 0.9f)
        val cue = advisor.analyze("I am happy but also frustrated and a bit scared")
        // Three categories with comparable scores → confidence margin is small.
        // At 0.9 threshold, the advisor stays silent.
        assertNull(cue)
    }

    @Test
    fun `evidence is omitted when maxEvidenceFragments is zero`() = runTest {
        val advisor = LexicalEmotionalAdvisor(maxEvidenceFragments = 0)
        val cue = advisor.analyze("I'm so frustrated and stuck again")
        assertNotNull(cue)
        assertTrue(cue.evidence.isEmpty(), "evidence should be empty when maxEvidenceFragments=0")
    }

    @Test
    fun `evidence is bounded by maxEvidenceFragments`() = runTest {
        val advisor = LexicalEmotionalAdvisor(maxEvidenceFragments = 2)
        // Many frustration triggers in one input.
        val cue = advisor.analyze("frustrated frustrated stuck stuck again again not working")
        assertNotNull(cue)
        assertTrue(cue.evidence.size <= 2, "evidence should be bounded, got ${cue.evidence.size}")
    }

    @Test
    fun `custom lexicon overrides default`() = runTest {
        val customLexicon = Lexicon.fromKeywords(
            mapOf(Emotion.CURIOSITY to listOf("\\bquark\\b", "\\bphoton\\b"))
        )
        val advisor = LexicalEmotionalAdvisor(
            lexicon = customLexicon,
            confidenceThreshold = 0.0f,  // accept anything
        )
        val cue = advisor.analyze("the photon and the quark walked into a bar")
        assertNotNull(cue)
        assertEquals(Emotion.CURIOSITY, cue.emotion)
    }

    @Test
    fun `intensity is normalized when one category dominates`() = runTest {
        val advisor = LexicalEmotionalAdvisor()
        // Only frustration triggers, multiple times → high intensity.
        val cue = advisor.analyze("frustrated and stuck and not working, this stupid thing")
        assertNotNull(cue)
        assertEquals(Emotion.FRUSTRATION, cue.emotion)
        assertEquals(1.0f, cue.intensity, 0.001f)
    }
}
