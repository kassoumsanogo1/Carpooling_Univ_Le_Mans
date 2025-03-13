package com.covoiturage.covoiturage.web_service;

import com.covoiturage.covoiturage.controller.UserController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.*;

public class API_EXT_INE {
    Logger logger = LoggerFactory.getLogger(UserController.class);

    private void disableSSLVerification() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getJson(URL url) {
        disableSSLVerification(); // Add this line to disable SSL verification
        try (InputStream input = url.openStream()) {
            InputStreamReader isr = new InputStreamReader(input);
            BufferedReader reader = new BufferedReader(isr);
            StringBuilder json = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                json.append((char) c);
            }
            return json.toString();
        } catch (IOException e) {
            logger.error("Erreur lors de l'appel à l'URL: " + url + " - " + e.getMessage());
            throw new RuntimeException("Erreur lors de l'appel à l'API: " + e.getMessage(), e);
        }
    }

    public List<Integer> getListUsersAPI() throws MalformedURLException, JsonProcessingException {
        URL url=new URL("https://c363cd03-adfa-4394-8a07-3a0a269acdf5.mock.pstmn.io/numeroEtudiant");
        String EtudiantsAPI=this.getJson(url);
        ObjectMapper mapper = new ObjectMapper();
        List<Integer> listUsersAPI = Arrays.asList(mapper.readValue(EtudiantsAPI, Integer[].class));
        return listUsersAPI;
    }

    public List<Double> coordinateAPI(String origine, String destination) throws JsonProcessingException {
        System.out.println("Recherche des coordonnées pour: " + origine + " -> " + destination);
        
        RestTemplate restTemplate = new RestTemplate();
        
        // Encodage des adresses pour l'URL
        String encodedOrigine = origine.replace(" ", "+");
        String encodedDestination = destination.replace(" ", "+");
        
        // Appel à l'API pour l'origine
        String urlOrigine = "https://api-adresse.data.gouv.fr/search/?q=" + encodedOrigine + "&limit=1";
        ResponseEntity<Map> responseOrigine = restTemplate.getForEntity(urlOrigine, Map.class);
        
        // Appel à l'API pour la destination
        String urlDestination = "https://api-adresse.data.gouv.fr/search/?q=" + encodedDestination + "&limit=1";
        ResponseEntity<Map> responseDestination = restTemplate.getForEntity(urlDestination, Map.class);
        
        List<Double> coords = new ArrayList<>();
        
        try {
            // Extraction des coordonnées de l'origine
            List<Map<String, Object>> featuresOrigine = (List<Map<String, Object>>) responseOrigine.getBody().get("features");
            if (!featuresOrigine.isEmpty()) {
                Map<String, Object> geometryOrigine = (Map<String, Object>) featuresOrigine.get(0).get("geometry");
                List<Double> coordinatesOrigine = (List<Double>) geometryOrigine.get("coordinates");
                coords.add(coordinatesOrigine.get(1)); // latitude
                coords.add(coordinatesOrigine.get(0)); // longitude
            }
            
            // Extraction des coordonnées de la destination
            List<Map<String, Object>> featuresDestination = (List<Map<String, Object>>) responseDestination.getBody().get("features");
            if (!featuresDestination.isEmpty()) {
                Map<String, Object> geometryDestination = (Map<String, Object>) featuresDestination.get(0).get("geometry");
                List<Double> coordinatesDestination = (List<Double>) geometryDestination.get("coordinates");
                coords.add(coordinatesDestination.get(1)); // latitude
                coords.add(coordinatesDestination.get(0)); // longitude
            }
            
            System.out.println("Coordonnées trouvées: " + coords);
            return coords;
            
        } catch (Exception e) {
            System.out.println("Erreur lors de l'extraction des coordonnées: " + e.getMessage());
            throw new JsonProcessingException(e.getMessage()) {};
        }
    }

    public String dureeAPI(double latitude1,double longitude1, double latitude2,double longitude2) throws JsonProcessingException {
        System.out.println("Début de dureeAPI avec coordonnées:");
        System.out.println("Origine: " + latitude1 + "," + longitude1);
        System.out.println("Destination: " + latitude2 + "," + longitude2);
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization","prj_live_pk_280ee05fafb20726b59d20ca99f945c6d6a4c2d9");
        
        String createPersonUrl = "https://api.radar.io/v1/route/matrix?origins="+latitude1+","+longitude1+"&destinations="+latitude2+","+longitude2+"&mode=car&units=metric";
        System.out.println("URL de l'API: " + createPersonUrl);
        
        HttpEntity<String> request = new HttpEntity<String>(headers);
        System.out.println("Headers: " + headers);
        
        try {
            ResponseEntity<HashMap> result = restTemplate.exchange(createPersonUrl, HttpMethod.GET, request, HashMap.class);
            System.out.println("Code de réponse: " + result.getStatusCode());
            System.out.println("Corps de la réponse: " + result.getBody());
            
            Map<String,ArrayList<ArrayList<Map<String,Map<String,String>>>>> list2=result.getBody();
            String duree=list2.get("matrix").get(0).get(0).get("duration").get("text");
            System.out.println("Durée extraite: " + duree);
            return duree;
        } catch (Exception e) {
            System.out.println("Erreur lors de l'appel API: " + e.getMessage());
            e.printStackTrace();
            throw new JsonProcessingException(e.getMessage()) {};
        }
    }


    public  List<Double> coordinateAPI1(String origine, String destionation) throws MalformedURLException, JsonProcessingException {
        URL url=new URL("https://fr.distance24.org/route.json?stops="+origine+"|"+destionation+"");
        String reponse= this.getJson(url);
        Map<String, ArrayList<Map<String,Double>>>  map = new ObjectMapper().readValue(reponse, HashMap.class);
        double latutude1=map.get("stops").get(0).get("latitude");
        double longitude1=map.get("stops").get(0).get("longitude");
        double latutude2=map.get("stops").get(1).get("latitude");
        double longitude2=map.get("stops").get(1).get("longitude");
        List<Double> coords= new ArrayList<>();
        coords.add(latutude1);
        coords.add(longitude1);
        coords.add(latutude2);
        coords.add(longitude2);
        return coords;
    }
    
}




