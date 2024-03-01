# Google Cloud AI Connector

A custom Camunda outbound connector to connect with the Google Cloud AI and perform operations with it
## Google Cloud AI Connector Operations

| Operation           | Functionality                                             |
|---------------------|-----------------------------------------------------------|
| Prompt               | Execute the Vertex AI prompt functionality with PaLM 2 text generative AI models          |
| Pretrained Image Labeling | Identifies the class of objects present in an image and gives the confidence|
| Pretrained Object Tracking in a video | Identifies the class of objects present in a video and gives the confidence|
| Run a batch prediction on a custom trained model | Creates a batch prediction request in a custom models trained on Vertex AI|
| Get job state | Gets the status of the batch prediction job that is trigerred on a custom trained Vertex AI model|
| Get Output URI | Gives the GCS URI for the resultant jsonl file that is generated as a result of batch predictions|

## Prerequisites
- Access credentials for your Google Cloud Storage project can be obtained through a Service Account. 
- To create the necessary Service Account, refer to the [Developer's Guide for creating a Google service account](https://developers.google.com/workspace/guides/create-credentials#service-account)
- The IAM Service Account requires a role with appropriate permissions for object operations within a bucket. You can find more information about [cloud storage roles](https://cloud.google.com/storage/docs/access-control/iam-roles) and [vertex AI permissions](https://cloud.google.com/vertex-ai/docs/general/access-control) 
- Follow the [Developers guide for creating credentials for a service account](https://developers.google.com/workspace/guides/create-credentials#create_credentials_for_a_service_account) to create and download a JSON key file.

- The JSON file is formatted as follows
```json
{
    "type": "service_account",
    "project_id": "",
    "private_key_id": "",
    "private_key": "-----BEGIN PRIVATE KEY-----\n ...\n-----END PRIVATE KEY-----\n",
    "client_email": "",
    "client_id": "",
    "auth_uri": "",
    "token_uri": "",
    "auth_provider_x509_cert_url": "",
    "client_x509_cert_url": "",
    "universe_domain": "googleapis.com"
  }
  
```
## Configuration
### Configure Runtime
<li> Generate the jar file

```
mvn clean package
```

<li>Build a docker image

```
docker build -t gcai-connector .
```
<li>Dockerfile :

```
FROM camunda/connectors:8.4.1
COPY target/google-cloud-ai-jar-with-dependencies.jar /opt/app/
```
<li>Build a docker runtime

  &emsp;&emsp;&emsp;Configure your runtime as per your installation from <a href="https://docs.camunda.io/docs/self-managed/connectors-deployment/connectors-configuration/">here</a> and start a container of the built docker image.

### Element Template

&emsp;&emsp;&emsp;The connector element template that should be used for this connector can be found as a JSON file in the "element-templates" folder.

## Functionalities
### Execute Prompt
> This operation gets the query input from the user and executes it with Vertex Generative AI - Prompt and outputs the result to the given query. 

<img src="assets/images/prompt.png" alt="prompt" width="400"/>

### Pretrained Image Labeling
> This operation loads an image, utilizes the pretrained Vision AI Model that is present in GCP for Image Labeling, make predictions and retu the class name and confidence of the prediction.

<img src="assets/images/image label.png" alt="image labeling" width="400"/>

### Pretrained Object Tracking in a video
> This operation loads a video, utilizes the pretrained Cloud Video Intelligence AI Model that is present in GCP for Object Tracking, make predictions and return the class name, segment(start time and end time) and confidence of 20 predictions with confidence more than 0.8

<img src="assets/images/video tracking.png" alt="object tracking" width="400"/>

### Run a batch prediction on a custom trained model
> This operation performs an asynchronous batch prediction operation on a custom trained AI model on Vertex AI, intaking the model ID, a source jsonl file and the location. It is a necessity that the jsonl file located in GCS and the custom model trained on Vertex AI should be of the same region.

<img src="assets/images/custom.png" alt="custom batch prediction"/>

### Get job state
> This operation fetches the current job state of the batch prediction job that is executed on a custom trained AI model on Vertex AI, intaking the job ID, project name and the location.

<img src="assets/images/job state.png" alt="job state"/>

### Get Output URI
> This operation waits for the current job state of the batch prediction job that is executed on a custom trained AI model on Vertex AI to succeed, intaking the job ID, project name and the location, and returns the GCS URI where the output jsonl file is generated as a result of prediction. Else, it just returns the job state.

<img src="assets/images/output uri.png" alt="output uri"/>


## Sample BPMN

You can find the sample BPMN file in the `assets/bpms/` folder of this directory.

## Contact Information
For any queries and further support, please drop us a mail at camunda.support@acheron-tech.com