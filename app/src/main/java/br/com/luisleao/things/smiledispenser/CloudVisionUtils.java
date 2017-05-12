package br.com.luisleao.things.smiledispenser;
/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

        import android.util.Log;

        import com.google.api.client.extensions.android.http.AndroidHttp;
        import com.google.api.client.googleapis.json.GoogleJsonResponseException;
        import com.google.api.client.http.HttpTransport;
        import com.google.api.client.json.JsonFactory;
        import com.google.api.client.json.gson.GsonFactory;
        import com.google.api.services.vision.v1.Vision;
        import com.google.api.services.vision.v1.VisionRequestInitializer;
        import com.google.api.services.vision.v1.model.AnnotateImageRequest;
        import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
        import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
        import com.google.api.services.vision.v1.model.EntityAnnotation;
        import com.google.api.services.vision.v1.model.FaceAnnotation;
        import com.google.api.services.vision.v1.model.Feature;
        import com.google.api.services.vision.v1.model.Image;

        import java.io.IOException;
        import java.util.Arrays;
        import java.util.Collections;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Map;

public class CloudVisionUtils {
    public static final String TAG = CloudVisionUtils.class.getSimpleName();

    private static final String CLOUD_VISION_API_KEY = "AIzaSyBJ4v-mfMfMeMafKOVPx6qgfpZISmfmtIw";

    private static final String LABEL_DETECTION = "LABEL_DETECTION";
    private static final String FACE_DETECTION = "FACE_DETECTION";
    private static final String LOGO_DETECTION = "LOGO_DETECTION";

    private static final int MAX_FACE_RESULTS = 3;
    private static final int MAX_LABEL_RESULTS = 10;
    private static final int MAX_LOGO_RESULTS = 3;

    /**
     * Construct an annotated image request for the provided image to be executed
     * using the provided API interface.
     *
     * @param imageBytes image bytes in JPEG format.
     * @return collection of annotation descriptions and scores.
     */
    public static Map<String, Object> annotateImage(byte[] imageBytes) throws IOException { //Map<String, String> //BatchAnnotateImagesResponse


        try {

            // Construct the Vision API instance
            HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            Log.i(TAG, "TESTANDO UPLOAD VISION...");

            VisionRequestInitializer initializer = new VisionRequestInitializer(CLOUD_VISION_API_KEY);
            Vision vision = new Vision.Builder(httpTransport, jsonFactory, null)
                    .setVisionRequestInitializer(initializer)
                    .setApplicationName("SmileDispenser")
                    .build();

            // Create the image request
            AnnotateImageRequest imageRequest = new AnnotateImageRequest();
            Image img = new Image();
            img.encodeContent(imageBytes);
            imageRequest.setImage(img);

            // Add the features we want
            Feature faceDetection = new Feature();
            faceDetection.setType(FACE_DETECTION);
            faceDetection.setMaxResults(MAX_FACE_RESULTS);

            Feature labelDetection = new Feature();
            labelDetection.setType(LABEL_DETECTION);
            faceDetection.setMaxResults((MAX_LABEL_RESULTS));

            Feature logoDetection = new Feature();
            logoDetection.setType(LOGO_DETECTION);
            logoDetection.setMaxResults(MAX_LOGO_RESULTS);


            Feature[] features = { faceDetection, labelDetection, logoDetection };

            imageRequest.setFeatures(Arrays.asList(features));
            //imageRequest.setFeatures(Collections.singletonList(faceDetection));

            // Batch and execute the request
            BatchAnnotateImagesRequest requestBatch = new BatchAnnotateImagesRequest();
            requestBatch.setRequests(Collections.singletonList(imageRequest));
            BatchAnnotateImagesResponse response = vision.images()
                    .annotate(requestBatch)
                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    .setDisableGZipContent(true)
                    .execute();

            return convertResponseToMap(response);

        } catch (GoogleJsonResponseException e) {
            Log.d(TAG, "failed to make API request because " + e.getContent());
        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " +
                    e.getMessage());
        }
        return null;
    }

    /**
     * Process an encoded image and return a collection of vision
     * annotations describing features of the image data.
     *
     * @return collection of annotation descriptions and scores.
     */
    private static Map<String, Object> convertResponseToMap(BatchAnnotateImagesResponse response) {

        // Convert response into a readable collection of annotations
        Map<String, Object> annotations = new HashMap<>();
        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
        if (labels != null) {
            for (EntityAnnotation label : labels) {
                annotations.put(label.getDescription(), label.getScore());
            }
        }

        List<FaceAnnotation> faces = response.getResponses().get(0).getFaceAnnotations();
        if (faces != null) {
            annotations.put("faces", faces.size());
            for (FaceAnnotation face : faces) {
                annotations.put("face_Joy", face.get("joyLikelihood")); //face.getJoyLikelihood()
                annotations.put("face_Anger", face.get("angerLikelihood")); //face.getAngerLikelihood()
                annotations.put("face_Blurred", face.get("blurredLikelihood")); //face.getBlurredLikelihood()
                annotations.put("face_Headwer", face.get("headwearLikelihood")); //face.getHeadwearLikelihood()
                annotations.put("face_Sorrow", face.get("sorrowLikelihood")); //face.getSorrowLikelihood()
                annotations.put("face_Surprise", face.get("surpriseLikelihood")); //face.getSurpriseLikelihood()
                annotations.put("face_UnderExposed", face.get("underExposedLikelihood")); //face.getUnderExposedLikelihood()
                break;
            }
        } else {
            annotations.put("faces", 0);
        }
        List<EntityAnnotation> logos = response.getResponses().get(0).getLogoAnnotations();
        if (logos != null) {
            for (EntityAnnotation logo : logos) {
                annotations.put(logo.getDescription().replace(" ", "").toLowerCase(), logo.getScore());
            }
        }

        Log.d(TAG, "Cloud Vision request completed:" + annotations);
        return annotations;
    }
}