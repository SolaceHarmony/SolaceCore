"""
This script performs an incremental synchronization of Jira issues to Delta tables using Spark.
It includes the following main components:
1. **Logging Configuration**: Sets up logging with info level by default.
2. **Spark Session Initialization**: Initializes a Spark session with Delta Lake configurations.
3. **JiraHandler Class**: Handles interactions with Jira, including fetching issue types, fields, and issues.
4. **JiraPrimitives Class**: Provides low-level methods for interacting with Jira's REST API.
5. **DataFrameHandler Class**: Processes Jira issues into Spark DataFrames, including flattening JSON and translating
   special fields.
6. **DeltaTableHandler Class**: Manages Delta tables, including upserting data and handling metadata for synchronization
7. **Main Execution Block**: Orchestrates the synchronization process, including fetching issues, processing them, and
   upserting them into Delta tables.
Classes:
    JiraHandler: Handles Jira interactions, including fetching issue types, fields, and issues.
    JiraPrimitives: Provides low-level methods for interacting with Jira's REST API.
    DataFrameHandler: Processes Jira issues into Spark DataFrames.
    DeltaTableHandler: Manages Delta tables, including upserting data and handling metadata for synchronization.
Functions:
    enable_debug_logging: Enables debug-level logging for detailed tracing.
    main: Orchestrates the synchronization process, including fetching issues, processing them, and upserting them into
          Delta tables.
Attributes:
    CREDENTIALS_BASE_PATH (str): Base path for credentials.
    logger (Logger): Logger instance for logging messages.
    testing_mode (bool): Flag to indicate if the script is running in testing mode.
    spark (SparkSession): Spark session instance.
"""

import json
import logging
import base64
import pandas as pd
import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
from pyspark.sql import SparkSession
from pyspark.sql.functions import col
from datetime import datetime, timezone
from pyspark.sql.types import StructField, StructType, StringType
from pyspark.sql.utils import AnalysisException
from urllib.parse import quote_plus

CREDENTIALS_BASE_PATH = "/lakehouse/default/Files"

# Set up logging with info level by default
logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)
testing_mode = True


def enable_debug_logging():
    """
    This function sets the logging level to DEBUG and logs a message indicating
    that debug logging has been enabled. This is useful for troubleshooting and
    getting detailed information about the program's execution.

    Usage:
        Call this function at the beginning of your script or before the code
        section you want to debug.

    Example:
        enable_debug_logging()
    Enable debug-level logging for detailed tracing.
    """
    logger.setLevel(logging.DEBUG)
    logger.debug("Debug logging is now enabled.")


"""
### Spark Session Initialization

spark = SparkSession.builder

.appName("Incremental Jira Sync")
Sets the name of the Spark application ("Incremental Jira Sync").
This name will appear in the Spark web UI and logs, making it easier to identify the application.

.config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
Adds the Delta Lake extension to the Spark session. This extension enables Delta Lake-specific features and functions 
within Spark SQL.

.config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
Configures the Spark session to use the Delta Lake catalog as the default catalog for managing tables. This setting 
allows Spark to recognize and interact with Delta tables.

.config("spark.databricks.delta.schema.autoMerge.enabled", "true")
Enables automatic schema merging during Delta Lake operations. When set to true, Delta Lake will automatically merge 
schemas when new data with different schema is written to an existing Delta table. This is useful for handling evolving 
data schemas without manual intervention.

.getOrCreate()
Creates a new Spark session or gets an existing one if it already exists. This method is used to initialize the Spark 
session with the specified configurations.
 """

spark = (
    SparkSession.builder.appName("Incremental Jira Sync")
    .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
    .config(
        "spark.sql.catalog.spark_catalog",
        "org.apache.spark.sql.delta.catalog.DeltaCatalog",
    )
    .config("spark.databricks.delta.schema.autoMerge.enabled", "true")
    .getOrCreate()
)
from delta.tables import DeltaTable


class JiraHandler:
    """
    JiraHandler is a class designed to handle interactions with Jira, including fetching issue types, fields, and
    issues.
    Attributes:
        credentials_path (str): Path to the credentials file.
        delta_handler (DeltaTableHandler): An instance of DeltaTableHandler to manage delta operations.
        username (str): Jira username.
        token (str): Jira API token.
        instance_blacklist (set): A set of blacklisted Jira instances.
        jira_primitives (JiraPrimitives): An instance of JiraPrimitives to interact with Jira APIs.
        issuetypes_by_project (dict): A dictionary to store issue types by project.
        jiradefaults_by_project (dict): A dictionary to store default fields by project.
        dataframe_handler (DataFrameHandler): An instance of DataFrameHandler to process issues.
        names_dict (dict): A dictionary to store names of fields.
        projects (list): A list of projects keys given an instance.
    Constructor:
        __init__(self, credentials_path, delta_handler):
            Initializes the JiraHandler with the given credentials path and delta handler.
    Methods:
        connect_instance(self, instance):
            Connects to a Jira instance and fetches default fields and issue types.
        set_dataframe_handler(self, dataframe_handler):
            Sets the DataFrameHandler instance to use for processing issues.
        get_issue_types(self, instance, project_key):
            Fetches all issue types for a project from Jira.
        fetch_issuetype_fields(self, instance, project):
        fetch_default_fields(self, instance, project):
        extract_default_fields(self, field_data, project):
            Processes the field data to extract default fields.
        fetch_issues(self, instance, jql_query, project, issue_type=None, start_at=0):
            Fetches issues from a Jira instance using a JQL query.
        load_credentials(self):
            Loads credentials from a JSON file.
    """

    def __init__(self, credentials_filepath, deltahandler_instance=None):
        """
        Initializes the JiraHandler with the given credentials path and delta handler.
        Args:
            credentials_filepath: Path to the credentials file.
            deltahandler_instance (DeltaTableHandler): An instance of DeltaTableHandler to manage delta operations.
        """
        self.default_fields = None
        self.credentials_path = credentials_filepath  # Path to the credentials file
        if not deltahandler_instance:
            # If no DeltaHandler is provided, set it to None - no blacklisting will be done
            raise ValueError("DeltaTableHandler instance is required for blacklisting.")

        self.delta_handler = deltahandler_instance  # An instance of DeltaTableHandler to manage delta operations
        self.username = None  # Jira username
        self.token = None  # Jira API token

        # Load credentials from the JSON file
        self.load_credentials()

        # Check if credentials are loaded successfully
        if not self.username or not self.token:
            raise ValueError(
                "Credentials file could not be loaded. Jira configuration needed."
            )

        # DeltaTableHandler.blacklist(): Load or create blacklist from Delta table 'blacklist'
        self.instance_blacklist = self.delta_handler.get_blacklist()
        self.jira_primitives = JiraPrimitives(
            self.username, self.token
        )  # An instance of JiraPrimitives to interact with Jira APIs
        self.issuetypes_by_project = {}  # A dictionary to store issue types by project
        self.jiradefaults_by_project = (
            {}
        )  # A dictionary to store default fields by project
        self.dataframe_handler = None  # Will be set after initialization
        self.names_dict = {}  # A dictionary to store names of fields
        self.projects = []  # A list of projects keys given an instance
        logger.info(
            f"Initialized JiraHandler with credentials from: {credentials_filepath}"
        )

    def connect_instance(self, instance):
        """
        Connects to a Jira instance and fetches default fields and issue types.
        Args:
            instance (str): The Jira instance (e.g. lumen-sre, lumen, lumen-nm) to connect to.
        """
        self.default_fields = (
            self.jira_primitives.fetch_default_fields()
        )  # Fetch default fields
        self.issuetypes_by_project = self.jira_primitives.fetch_issuetype_fields(
            instance, key_for_project
        )  # Fetch issue types
        self.instance_blacklist = (
            self.delta_handler.get_blacklist()
        )  # Get the instance blacklist
        logger.info(
            f"Connected to Jira instance {instance}"
        )  # Log the successful connection

    def set_dataframe_handler(self, dataframe_handler_instance):
        """
        Set the DataFrameHandler instance to use for processing issues.
        Args:
            dataframe_handler_instance (DataFrameHandler): An instance of DataFrameHandler.
        """
        self.dataframe_handler = (
            dataframe_handler_instance  # Set the DataFrameHandler instance
        )
        logger.info("Set DataFrameHandler for JiraHandler")

    def get_issue_types(self, instance, project_key):
        """
        Fetch all issue types for a project from Jira.
        Args:
            instance (str): The Jira instance.
            project_key (str): The project key.
        Returns:
            list: A list of issue types for the project.
        Data format for issue types:
        [
            {
                "name": "Task",
                "id": "10001",
                "statusCategory": {
                    "colorName": "blue-gray",
                    "name": "To Do",
                    "key": "new"
                }
            },
            {
                "name": "Story",
                "id": "10002",
                "statusCategory": {
                    "colorName": "blue-gray",
                    "name": "To Do",
                    "key": "new"
                }
            }
        """
        if instance.lower() in self.instance_blacklist:
            logger.warning(
                f"Instance '{instance}' is blacklisted. Skipping issue type fetch."
            )
            return []
        return self.jira_primitives.fetch_issue_types(instance, project_key)

    def fetch_issuetype_fields(self, instance, key_for_project):
        """
        Fetches the allowed fields for each issue type from a Jira project using createmeta API.
        Args:
            instance (str): The Jira instance.
            key_for_project (str): The project key.
        Returns:
            dict: A dictionary of issue types with their fields.
        Data format for issue types:
        {
            "10001": {
                "name": "Task",
                "fields": {
                    "summary": "summary",
                    "description": "description",
                    ...
                }
            },
            "10002": {
                "name": "Story",
                "fields": {
                    "summary": "summary",
                    "description": "description",
                    ...
                }
            }
        }
        """
        if (
            instance.lower() in self.instance_blacklist
        ):  # Check if the instance is blacklisted
            return None  # Return None if the instance is blacklisted
        if (
            key_for_project in self.issuetypes_by_project
        ):  # Check if the issue types are already fetched for the project
            return self.issuetypes_by_project[
                key_for_project
            ]  # Return the cached issue types
        issuetypes_data = self.jira_primitives.fetch_issuetype_fields(
            instance, key_for_project
        )  # Fetch issue types
        self.issuetypes_by_project[key_for_project] = (
            issuetypes_data  # Cache the issue types
        )
        return issuetypes_data  # Return the issue types

    def fetch_default_fields(self, instance, project):
        """
        Fetches the default fields from a Jira instance using fields API.
        Args:
            instance (str): The Jira instance.
            project (str): The project key.
        Returns:
            set: A set of default fields for the project.
        Data format for default fields:
        [
            {
                "id": "summary",
                "name": "Summary",
                "custom": false
            },
            {
                "id": "description",
                "name": "Description",
                "custom": false
            },
            ...
        ]
        """
        if (
            instance.lower() in self.instance_blacklist
        ):  # Check if the instance is blacklisted
            return None  # Return None if the instance is blacklisted
        if (
            project in self.jiradefaults_by_project
        ):  # Check if the default fields are already fetched for the project
            return self.jiradefaults_by_project[
                project
            ]  # Return the cached default fields
        field_data = self.jira_primitives.fetch_default_fields(
            instance
        )  # Fetch default fields
        default_fields = self.extract_default_fields(
            field_data, project
        )  # Extract default fields
        self.jiradefaults_by_project[project] = (
            default_fields  # Cache the default fields
        )
        return default_fields  # Return the default fields

    def extract_default_fields(self, field_data, project):
        """
        Process the field data to extract default fields.
        Args:
            field_data (list): A list of field data from the Jira API.
            project (str): The project key.
        Returns:
            set: A set of default fields for the project.
        Data format for field data:
        [
            {
                "id": "summary",
                "name": "Summary",
                "custom": false
            },
            {
                "id": "description",
                "name": "Description",
                "custom": false
            },
            ...
        ]
        """
        default_fields = {
            field["id"]
            for field in field_data  # Extract default fields
            if not field["custom"] or field["name"] in ["Flagged", "Story Points"]
        }  # Include custom fields
        self.jiradefaults_by_project[project] = (
            default_fields  # Cache the default fields
        )
        return default_fields  # Return the default fields

    def fetch_issues(self, instance, jql_query, project, issue_type=None, start_at=0):
        """
        Fetch issues from a Jira instance using a JQL query.

        Args:
            instance (str): The Jira instance.
            jql_query (str): The JQL query to fetch issues.
            start_at (int): The starting index for pagination.

        Returns:
            list: A list of issues fetched from Jira.
        """
        if (
            issue_type and f'issuetype = "{issue_type}"' not in jql_query
        ):  # Check if the issue type is specified in the JQL query
            jql_query += f' AND issuetype = "{issue_type}"'  # Add the issue type to the JQL query
        issues_fetched = self.jira_primitives.fetch_issues(jql_query)  # Fetch issues
        if issues_fetched is None:  # Check if issues are fetched successfully
            return None  # Return None if there are no issues
        self.names_dict = (
            issues_fetched[0]["names"] if issues_fetched else {}
        )  # Extract field names
        logger.info(
            f"Fetched {len(issues_fetched)} issues for project {project}"
            + (f" and issue type {issue_type}" if issue_type else "")
        )  # Log the number of fetched issues
        return issues_fetched  # Return the fetched issues

    def load_credentials(self):
        """
        Load credentials from JSON file.

        Properties set:
            username (str): Jira username.
            token (str): Jira API token.
            projects (list): List of Jira projects.

        File format:
        {
            "username": "your_username",
            "token": "your_token",
            "projects": [
                {
                    "instance": "your_instance",
                    "project": "your_project"
                }
            ]
        }
        Raises:
            Exception: If the credentials file cannot be loaded.
        """
        try:
            credentials_file_path = f"{CREDENTIALS_BASE_PATH}/{self.credentials_path}"  # Construct the credentials file path
            with open(credentials_file_path, "r") as f:  # Open the credentials file
                credentials = json.load(f)  # Load the credentials from the file
                self.username = credentials["username"]  # Set the Jira username
                self.token = credentials["token"]  # Set the Jira API token
                self.projects = credentials["projects"]  # Set the list of Jira projects
                logger.info(
                    f"Loaded credentials for {len(self.projects)} projects."
                )  # Log the successful loading of credentials
        except Exception as e:  # Handle exceptions
            logger.error(
                f"Failed to load credentials: {e}"
            )  # Log an error if the credentials file cannot be loaded
            raise e  # Raise an exception


class JiraSession:
    """
    JiraSession manages HTTP interactions with the Jira API.
    Attributes:
        base_url (str): The base URL for the Jira instance.
        headers (dict): The headers for authorization.
        session (requests.Session): A session object with retry logic.
    """

    def __init__(self, instance, username, token):
        self.base_url = f"https://{instance}.atlassian.net"
        self.headers = self._get_headers(username, token)
        self.session = self._create_session()

    def _create_session(self):
        session = requests.Session()
        retries = Retry(
            total=5, backoff_factor=1, status_forcelist=[500, 502, 503, 504]
        )
        adapter = HTTPAdapter(max_retries=retries)
        session.mount("https://", adapter)
        return session

    def _get_headers(self, username, token):
        credentials = f"{username}:{token}"
        encoded_credentials = base64.b64encode(credentials.encode()).decode()
        headers = {"Authorization": f"Basic {encoded_credentials}"}
        return headers

    def get(self, endpoint, params=None):
        url = f"{self.base_url}{endpoint}"
        try:
            response = self.session.get(url, headers=self.headers, params=params)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            logger.error(f"Request to {url} failed: {e}")
            return None


class JiraPrimitives:
    """
    JiraPrimitives is a class that provides API wrapper methods for interacting with Jira's REST API.

    Attributes:
        username (str): Jira username.
        token (str): Jira API token.
        session (requests.Session): A session object for making HTTP requests.

    Constructor:
        __init__(self, username, token):
            Initializes the JiraPrimitives with the given username and token.

    Methods:
        _create_session(self):
            Creates an HTTP session with retry logic.
        _get_headers(self):
            Constructs the headers for the HTTP requests.
        fetch_issue_types(self, instance, project_key):
            Fetches all issue types for a project from Jira.
        fetch_issuetype_fields(self, instance, project):
            Fetches the allowed fields for each issue type from a Jira project using createmeta API.
        fetch_default_fields(self, instance):
            Fetches the default fields from a Jira instance using fields API.
        fetch_issues(self, instance, jql_query, start_at=0):
            Fetches issues from a Jira instance using a JQL query.
    """

    def __init__(
        self, instance, username, token
    ):  # Initialize the JiraPrimitives with the given username and token
        self.jira_session = JiraSession(
            instance, username, token
        )  # Create a JiraSession object for making HTTP requests

    def fetch_issue_types(self, key_of_project):
        """
        Fetches all issue types for a project from Jira.
        Args:
            key_of_project (str): The project key.
        Returns:
            list: A list of issue types for the project.

        Jira API: Get all statuses for project
        Method: GET
        API Version: 2
        Documentation: https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-projects/#api-rest-api-2-project-projectidorkey-statuses-get

        Returns:
        - Returns the valid statuses for a project.
        - The statuses are grouped by issue type, as each project has a set of valid issue types and each issue type has a set of valid statuses.

        Example response:
            [
                {
                    "id": "3",
                    "name": "In Progress",
                    "self": "https://your-domain.atlassian.net/rest/api/2/issueType/3",
                    "statuses": [
                        {
                            "description": "The issue is currently being worked on.",
                            "iconUrl": "https://your-domain.atlassian.net/images/icons/progress.gif",
                            "id": "10000",
                            "name": "In Progress",
                            "self": "https://your-domain.atlassian.net/rest/api/2/status/10000"
                        },
                        {
                            "description": "The issue is closed.",
                            "iconUrl": "https://your-domain.atlassian.net/images/icons/closed.gif",
                            "id": "5",
                            "name": "Closed",
                            "self": "https://your-domain.atlassian.net/rest/api/2/status/5"
                        }
                    ],
                    "subtask": false
                }
            ]
        """
        endpoint = f"/rest/api/2/project/{key_of_project}/statuses"

        data = self.jira_session.get(endpoint)  # Fetch issue types

        if not data:  # Check if data is returned
            return []  # Return an empty list if no data is returned
        return [item["name"] for item in data]  # Return the list of issue types

    def fetch_issuetype_fields(self, projectkey, project):
        """
        Fetches the allowed fields for each issue type from a Jira project using createmeta API.
        Args:
            projectkey (str): The project key.
        Returns:
            dict: A dictionary of issue types with their fields.

        Jira API: Get create issue metadata
        Method: GET
        API Version: 2
        Documentation: https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-issue-createmeta/#api-rest-api-2-issue-createmeta-get
        :param project:

        """
        endpoint = "/rest/api/2/issue/createmeta?expand=projects.issuetypes.fields"
        data = self.jira_session.get(endpoint)
        if data:
            project_data = next(
                (item for item in data["projects"] if item["key"] == projectkey), None
            )
            if not project_data:
                logger.error(f"No project data found for {projectkey}")
                return None
            return {
                issuetype["id"]: {
                    "name": issuetype["name"],
                    "fields": set(issuetype["fields"].keys()),
                }
                for issuetype in project_data["issuetypes"]
            }
        return None

    def fetch_default_fields(self):
        endpoint = "/rest/api/3/field"
        return self.jira_session.get(endpoint)

    def fetch_issues(self, jql_query, start_at=0):
        encoded_jql = quote_plus(jql_query)
        issues_to_fetch = []
        while True:
            endpoint = "/rest/api/2/search"
            params = {
                "jql": encoded_jql,
                "maxResults": 100,
                "startAt": start_at,
                "expand": "names",
            }
            data = self.jira_session.get(endpoint, params=params)
            if not data or "issues" not in data:
                return issues_to_fetch
            issues_to_fetch.extend(data["issues"])
            start_at += len(data["issues"])
            if start_at >= data["total"]:
                break
        return issues_to_fetch


class DataFrameHandler:
    def __init__(self, spark):
        self.spark = spark
        self.special_fields = {
            "Flagged",
            "Linked Issues",
            "Labels",
            "Log Work",
            "Need Automation Help",
            "Old Solution",
            "Owner Team",
            "Sprint",
            "Status",
            "Sub-tasks",
            "Watchers",
            "Votes",
            "\u03a3 Original Estimate",
            "\u03a3 Progress",
            "\u03a3 Remaining Estimate",
            "\u03a3 Time Spent",
            "[CHART] Time in Status",
        }
        self.global_status_mapping_by_project = {}

    def normalize_field_name(self, field_name):
        """
        Normalize field names by removing special characters and converting to lowercase.
        """
        import re

        if field_name is None:
            logger.info(f"Received bad field name {field_name}")
            return None  # or an appropriate default, like 'unknown_field'

        normalized = re.sub(r"[^a-zA-Z0-9\s]", "", field_name)
        normalized = normalized.replace(" ", "_").lower()
        return normalized

    def translate_special_fields(self, issue, project):
        """
        Flattens the special fields in the Jira API response to make them easier to work with.
        """
        if project not in self.global_status_mapping_by_project:
            self.global_status_mapping_by_project[project] = {}

        translated = {}

        for field, value in issue.items():
            if field in self.special_fields:
                if field == "[CHART] Time in Status":
                    if value is not None:
                        clean_value = value.replace("*", "").replace("_", "")
                        segments = clean_value.split("|")
                        translated["[CHART] Time in Status"] = clean_value
                        status_id_array = []
                        for segment in segments:
                            status_id, dummy, status_time = segment.split(":")
                            status_id_array.append(status_id)
                            if (
                                status_id
                                in self.global_status_mapping_by_project[project]
                            ):
                                translated[
                                    f"time_in_{self.global_status_mapping_by_project[project][status_id]['name']}"
                                ] = status_time
                            else:
                                translated[f"time_in_status_{status_id}"] = status_time
                        # Handle statuses this issue doesn't have
                        for status_id in self.global_status_mapping_by_project[project]:
                            if status_id not in status_id_array:
                                translated[
                                    f"time_in_{self.global_status_mapping_by_project[project][status_id]['name']}"
                                ] = None
                    else:
                        for status_id in self.global_status_mapping_by_project[project]:
                            translated[
                                f"time_in_{self.global_status_mapping_by_project[project][status_id]['name']}"
                            ] = None
                            translated["[CHART] Time in Status"] = None
                elif field == "Flagged" and value is not None and len(value) > 0:
                    if value:
                        translated["flagged"] = value[0].get("value") == "Impediment"
                    else:
                        translated["flagged"] = None
                elif field == "Issue Type":
                    if value:
                        translated["issue_type"] = value.get("name")
                    else:
                        translated["issue_type"] = None
                elif field == "Sprint":
                    if value is None:
                        translated["sprint_ids"] = None
                        translated["sprint_names"] = None
                        translated["active_sprint"] = False
                        translated["multiple_sprints"] = False
                        translated["slotted_for_future_sprints"] = False
                    else:
                        translated["sprint_ids"] = [
                            sprint.get("id")
                            for sprint in value
                            if sprint.get("id") is not None
                        ]
                        translated["sprint_names"] = ",".join(
                            [sprint.get("name") for sprint in value]
                        )
                        translated["active_sprint"] = bool(
                            len(
                                [
                                    sprint.get("state")
                                    for sprint in value
                                    if sprint.get("state") == "active"
                                ]
                            )
                            > 0
                        )
                        translated["multiple_sprints"] = bool(len(value) > 1)
                        translated["slotted_for_future_sprints"] = bool(
                            len(
                                [
                                    sprint.get("state")
                                    for sprint in value
                                    if sprint.get("state") == "future"
                                ]
                            )
                            > 0
                        )
                else:
                    translated[field] = value

        return translated

    def flatten_json(self, y):
        out = {}

        def flatten(x, name=""):
            if isinstance(x, dict):
                for a in x:
                    flatten(x[a], f"{name}{a}.")
            elif isinstance(x, list):
                if all(
                    isinstance(elem, (str, int, float, bool, type(None))) for elem in x
                ):
                    out[name[:-1]] = ",".join(str(elem) for elem in x)
                else:
                    for i, a in enumerate(x):
                        flatten(a, f"{name}{i}.")
            else:
                out[name[:-1]] = x

        flatten(y)
        return out

    def process_issues(self, issues, names_dict):
        """
        Process issues into a DataFrame.
        """
        if not issues:
            logger.info("No issues to process.")
            return None
        normalized_field_name = ""
        flattened_issues = []
        for issue in issues:
            issue_data = {}
            issue_id = issue.get("id")
            issue_key = issue.get("key")
            issue_self = issue.get("self")

            issue_data["id"] = issue_id
            issue_data["key"] = issue_key
            issue_data["api_url"] = issue_self

            # Flatten the issue fields
            issue_fields = issue.get("fields", {})
            flattened_fields = self.flatten_json(issue_fields)

            # Normalize the field names
            for field, value in flattened_fields.items():
                if len(field.split(".")) > 0:
                    field_name = field.split(".")[0]
                else:
                    field_name = field

                if field_name is not None and field_name != "":
                    normalized_field_name = self.normalize_field_name(field_name)
                else:
                    logger.info(f"bad field name {field} to field name {field_name}")
                issue_data[normalized_field_name] = value

            # Translate special fields
            translated_issue = self.translate_special_fields(issue_data, names_dict)
            issue_data.update(translated_issue)

            flattened_issues.append(issue_data)

        df = pd.DataFrame(flattened_issues)
        issues_dataframe = self.spark.createDataFrame(df)
        logger.debug(f"Processed {issues_dataframe.count()} issues into DataFrame.")
        return issues_dataframe


class DeltaTableHandler:
    def __init__(self, spark, blacklist_table_name, metadata_table_name):
        self.spark = spark
        self.blacklist_table_name = blacklist_table_name
        self.metadata_table_name = metadata_table_name
        self.instance_blacklist = self.get_blacklist()

    def drop_blacklist_table(self):
        """
        Drop the blacklist table if it exists.
        """
        try:
            self.spark.sql(f"DROP TABLE IF EXISTS {self.blacklist_table_name}")
            logger.info(
                f"Blacklist table '{self.blacklist_table_name}' dropped successfully."
            )
        except Exception as e:
            logger.error(f"Failed to drop blacklist table: {e}")

    def get_blacklist(self):
        try:
            blacklist_df = self.spark.read.table(self.blacklist_table_name)
            blacklist_instances = set(
                blacklist_df.select("instance").rdd.flatMap(lambda x: x).collect()
            )
            logger.info(f"Loaded {len(blacklist_instances)} blacklisted instances")
            return blacklist_instances
        except Exception as e:
            logger.info(f"Blacklist table does not exist. Returning empty blacklist.")
            return set()

    def update_blacklist(self, new_instance):
        """
        Add a new instance to the blacklist.
        """
        try:
            blacklist_schema = StructType(
                [
                    StructField("instance", StringType(), False),
                    StructField("reason", StringType(), True),
                    StructField("blacklisted_at", StringType(), False),
                ]
            )
            new_entry = [
                (
                    new_instance,
                    "Authentication failure",
                    datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S%z"),
                )
            ]
            new_entry_df = self.spark.createDataFrame(
                new_entry, schema=blacklist_schema
            )

            try:
                # Try to append to existing table
                new_entry_df.write.format("delta").mode("append").saveAsTable(
                    self.blacklist_table_name
                )
            except AnalysisException:
                # Table doesn't exist, create it
                new_entry_df.write.format("delta").mode("overwrite").saveAsTable(
                    self.blacklist_table_name
                )

            logger.info(f"Added instance {new_instance} to the blacklist.")
        except Exception as e:
            logger.error(f"Failed to update blacklist: {e}")

    def get_sync_metadata(self, project_key, issue_type):
        """
        Get the last sync time for a specific project and issue type combination.
        """
        try:
            metadata_df = self.spark.read.table(self.metadata_table_name)
            metadata = (
                metadata_df.filter(
                    (col("project_key") == project_key)
                    & (col("issue_type") == issue_type)
                )
                .select("last_sync_time")
                .collect()
            )

            if metadata:
                last_sync_time = metadata[0][0]
                logger.info(
                    f"Last sync time for project '{project_key}' issue type '{issue_type}' is {last_sync_time}"
                )
                return last_sync_time
            else:
                return "1970-01-01 00:00"
        except Exception as e:
            # Create the metadata table with updated structure if it doesn't exist
            schema = StructType(
                [
                    StructField("project_key", StringType(), False),
                    StructField("issue_type", StringType(), False),
                    StructField("last_sync_time", StringType(), False),
                ]
            )
            data = [(project_key, issue_type, "1970-01-01 00:00")]
            df = self.spark.createDataFrame(data, schema)
            df.write.format("delta").mode("append").saveAsTable(
                self.metadata_table_name
            )
            logger.info(
                f"Created metadata entry for project '{project_key}' issue type '{issue_type}'"
            )
            return "1970-01-01 00:00"

    def update_sync_metadata(self, project_key, issue_type, new_sync_time):
        """
        Update the last sync time for a specific project and issue type combination.
        """
        data = [(project_key, issue_type, new_sync_time)]
        schema = StructType(
            [
                StructField("project_key", StringType(), False),
                StructField("issue_type", StringType(), False),
                StructField("last_sync_time", StringType(), False),
            ]
        )
        df = self.spark.createDataFrame(data, schema)

        try:
            delta_table = DeltaTable.forName(self.spark, self.metadata_table_name)
            (
                delta_table.alias("target")
                .merge(df.alias("source"))
                .whenMatchedUpdate(set={"last_sync_time": "source.last_sync_time"})
                .whenNotMatchedInsertAll()
                .execute()
            )
            logger.info(
                f"Updated sync metadata for project '{project_key}' issue type '{issue_type}'"
            )
        except AnalysisException:
            df.write.format("delta").mode("overwrite").saveAsTable(
                self.metadata_table_name
            )
            logger.info(
                f"Created metadata table for project '{project_key}' issue type '{issue_type}'"
            )

    def upsert_data(self, issues_df, project_key, issue_type):
        """
        Upsert data into a Delta table specific to the issue type.
        """
        if issues_df is None or issues_df.count() == 0:
            logger.info("No data to upsert. DataFrame is empty.")
            return

        issues_df = issues_df.filter(col("id").isNotNull())
        table_name = f"jira_issues_delta_{project_key.lower()}_{issue_type.lower().replace(' ', '_')}"

        try:
            delta_table = DeltaTable.forName(self.spark, table_name)
            (
                delta_table.alias("target")
                .merge(issues_df.alias("source"), "target.id = source.id")
                .whenMatchedUpdateAll()
                .whenNotMatchedInsertAll()
                .execute()
            )
            logger.info(f"Data upserted to Delta table '{table_name}'")
        except AnalysisException:
            issues_df.write.format("delta").saveAsTable(table_name)
            logger.info(
                f"Delta table '{table_name}' created with {issues_df.count()} records."
            )


if __name__ == "__main__":
    enable_debug_logging()

    credentials_path = "JSON/credentials.json"
    metadata_table_name = "jira_sync_metadata"
    blacklist_table_name = "blacklist_table"

    # Initialize handlers
    delta_handler = DeltaTableHandler(spark, blacklist_table_name, metadata_table_name)
    dataframe_handler = DataFrameHandler(spark)
    jira_handler = JiraHandler(credentials_path, delta_handler)

    # Set up cross-references
    jira_handler.set_dataframe_handler(dataframe_handler)

    if testing_mode:
        delta_handler.drop_blacklist_table()

    for project in jira_handler.projects:
        project_instance = project["instance"]
        project_key = project["project"]

        issuetype_fields = jira_handler.fetch_issuetype_fields(
            project_instance, project_key
        )

        # Fetch all issue types for the project
        issue_types = jira_handler.get_issue_types(project_instance)

        for issue_type in issue_types:
            last_sync_time = delta_handler.get_sync_metadata(project_key, issue_type)

            jql = f'project={project_key} AND updated >= "{last_sync_time}" AND issuetype = "{issue_type}" ORDER BY updated ASC'

            issues = jira_handler.fetch_issues(project_instance, jql, project_key)
            names_dict = jira_handler.names_dict
            if issues:
                # Process issues without depending on issue type mapping
                issues_df = dataframe_handler.process_issues(
                    issues, names_dict, project_key
                )

                # Find the issue type ID
                issue_type_id = next(
                    (
                        k
                        for k, v in issuetype_fields.items()
                        if v["name"].lower() == issue_type.lower()
                    ),
                    None,
                )

                if issue_type_id:
                    # Combine default fields with issue type specific fields
                    allowed_fields = (
                        jira_handler.default_fields
                        | issuetype_fields[issue_type_id]["fields"]
                    )

                if issues_df:
                    # Create/update table
                    delta_handler.upsert_data(issues_df, project_key, issue_type)
                    new_sync_time = datetime.now().strftime("%Y-%m-%d %H:%M")
                    delta_handler.update_sync_metadata(
                        project_key, issue_type, new_sync_time
                    )
            else:
                logger.info(
                    f"No issues of issue_type: {issue_type} in project {project_key}"
                )

    logger.info("Jira data sync complete.")
