// Kotlin Plan for Magentic Codex Core Architecture Overview
// Translated from ARCHITECTURE_OVERVIEW.md
// This file outlines the high-level architecture in Kotlin interfaces and classes

package com.solacecore.codexplans.architecture

/**
 * High-Level Architecture Interfaces
 * Based on the system architecture diagram from the document
 */

// Core Agent System
interface AgentLoop {
    fun run()
}

interface RunLoop {
    fun execute()
}

interface AgentState {
    val state: Map<String, Any>
}

interface ToolNegotiation {
    fun negotiate(tools: List<Tool>): List<Tool>
}

interface ToolRunner {
    fun runTool(tool: Tool): ToolResult
}

// Emotional Intelligence
interface MoodManager {
    fun manageMood(): Mood
}

interface SpikingNeuralNetwork {
    fun process(input: NeuralInput): NeuralOutput
}

interface MoodOrchestrator {
    fun makeDecision(context: EmotionalContext): Decision
}

interface MemoryConsolidation {
    fun consolidate(memory: Memory): ConsolidatedMemory
}

interface EmotionalContext {
    val valence: Double
    val arousal: Double
}

// Pipeline System
interface PipelineEngine {
    fun process(input: PipelineInput): PipelineOutput
}

interface PipelineConfig {
    val blocks: List<PipelineBlock>
}

interface PipelineBlock {
    fun execute(input: Any): Any
}

// Provider Layer
interface OllamaProvider : BaseProvider {
    fun connect(): Connection
}

interface OllamaClient {
    fun sendRequest(request: Request): Response
}

interface BaseProvider {
    val name: String
    fun isAvailable(): Boolean
}

// Tool System - MCP
interface MCPCore {
    fun executeTool(tool: Tool): ToolResult
}

interface MCPConverters {
    fun convert(input: Any): Any
}

interface MCPExecutor {
    fun execute(tool: Tool): ToolResult
}

interface MCPRegistry {
    fun register(tool: Tool)
    fun getTool(name: String): Tool?
}

interface MCPRouter {
    fun route(toolName: String): Tool?
}

interface StructuredTools {
    val tools: List<Tool>
}

// Data & History
interface Configuration {
    val settings: Map<String, Any>
}

interface NeutralHistoryXML {
    fun store(event: Event)
    fun retrieve(query: Query): List<Event>
}

interface NeutralMessages {
    fun send(message: Message)
}

interface EventBus {
    fun publish(event: Event)
    fun subscribe(subscriber: Subscriber)
}

interface FormatConverters {
    fun convert(from: Format, to: Format, data: Any): Any
}

// Safety & Approvals
interface ApprovalSystem {
    fun approve(action: Action): Boolean
}

interface RiskAssessment {
    fun assessRisk(action: Action): RiskLevel
}

// Data Classes
data class Tool(val name: String, val description: String)
data class ToolResult(val success: Boolean, val output: Any?)
data class Mood(val valence: Double, val arousal: Double)
data class NeuralInput(val spikes: List<Double>)
data class NeuralOutput(val activations: List<Double>)
data class Decision(val action: String, val confidence: Double)
data class Memory(val data: Any)
data class ConsolidatedMemory(val consolidated: Any)
data class PipelineInput(val data: Any)
data class PipelineOutput(val result: Any)
data class Connection(val status: String)
data class Request(val content: String)
data class Response(val content: String)
data class Event(val type: String, val data: Any)
data class Query(val criteria: Map<String, Any>)
data class Message(val content: String)
data class Subscriber(val callback: (Event) -> Unit)
enum class Format { JSON, XML, BINARY }
data class Action(val type: String, val parameters: Map<String, Any>)
enum class RiskLevel { LOW, MEDIUM, HIGH }

// Core Architectural Principles

/**
 * Dual-Model Cognition
 * Technical Brain and Emotional Core interfaces
 */
interface TechnicalBrain {
    fun reason(input: String): ReasoningResult
    fun executeTask(task: Task): TaskResult
}

interface EmotionalCore {
    fun processEmotion(input: EmotionalInput): EmotionalOutput
    fun modulateAttention(focus: Double): Double
}

data class ReasoningResult(val conclusion: String, val confidence: Double)
data class Task(val description: String)
data class TaskResult(val output: Any, val success: Boolean)
data class EmotionalInput(val stimulus: String)
data class EmotionalOutput(val emotion: Emotion)
data class Emotion(val type: String, val intensity: Double)