import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import com.esri.core.geometry.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.RAMDirectory;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.shapes.GHPoint;
import java.util.Date;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.sun.org.apache.xpath.internal.operations.Bool;
import joinery.DataFrame;



public class CustomMapMatching {
    GraphHopper hopper;
    String osmFileLocation = "osm-dublin/Porto.osm.pbf";
    String osmGraphFolders = "osm-dublin/graphfolderfiles";
    LocationIndex index;


//    class Polyline{
//        public void Polyline(Float[][] POLYLINE){
//
//        }
//    }

    private void initiateGraphhoper() {
        hopper = new GraphHopperOSM().forServer();
        hopper.setDataReaderFile(osmFileLocation);
        String link = "";

        hopper.setGraphHopperLocation(osmGraphFolders + "/" + osmFileLocation.hashCode() + "car");
        hopper.setEncodingManager(EncodingManager.create("car"));

        try {
            hopper.importOrLoad();
        } catch (Exception e) {
            System.err.println("[RealWorldStreetMovement] GraphHopper graph file must be imported again!");
            hopper.clean();
            hopper.importOrLoad();
        }
        LocationIndex locationIndex = new LocationIndexTree(hopper.getGraphHopperStorage(), new RAMDirectory());
        locationIndex.setResolution(20);
        locationIndex.prepareIndex();
        index = hopper.getLocationIndex();
    }

    private String getLinks(Float[] polyline) {
        List<GPXEntry> gpxList = new ArrayList<GPXEntry>();
        GHPoint ghPoint = new GHPoint(polyline[1], polyline[0]);
        Date date = new Date();
        Boolean invalid = false;
        String link;
        //This method returns the time in millis
        long timeMilli = date.getTime();
        GPXEntry gpxEntry = new GPXEntry(ghPoint, timeMilli);
        gpxList.add(gpxEntry);
        List<QueryResult> queryResults = new ArrayList<QueryResult>();
        for (int i = 0; i < gpxList.size(); i++) {
            GPXEntry gpx = gpxList.get(i);
            QueryResult closest = index.findClosest(gpx.getLat(), gpx.getLon(),
                    EdgeFilter.ALL_EDGES);
            if (!closest.isValid()){
                System.out.println("There was an error with Map Matchign!");
                return "";
            }
            queryResults.add(closest);
        }
        link = "" + queryResults.get(0).getClosestEdge().getEdge();
        link = link.trim();
        return link;
    }

    public void parseCsv(String pathToCsv){
        try {
            FileReader filereader = new FileReader(pathToCsv);
            FileWriter writer = new FileWriter("osm-dublin/modifiedtest.csv");

            writer.append("OriginalPolyline");
            writer.append(',');
            writer.append("EdgeIds");
            writer.append('\n');


            // create csvReader object and skip first Line
            CSVReader reader = new CSVReader(filereader);
            ArrayList<String> linkIds = new ArrayList<String>();
            ArrayList<ArrayList<String>> allLinks = new ArrayList<ArrayList<String>>();

            String [] nextLine;
            reader.readNext();
            while ((nextLine = reader.readNext()) != null) {
                // nextLine[] is an array of values from the line
                String nextPolyline = nextLine[1];

                ObjectMapper mapper = new ObjectMapper();
                Float[][] participantJsonList = mapper.readValue(nextPolyline, Float[][].class);

                for (Float[] coords:participantJsonList){
                    String result = getLinks(coords);
                    linkIds.add(result);
                }

                writer.append("\"" + nextPolyline+ "\"");
                writer.append(',');
                writer.append("\"" + linkIds.toString() + "\"");
                writer.append('\n');

                linkIds.clear();


            }

            writer.flush();
            writer.close();
            reader.close();

        }
        catch (Exception e) {
            System.out.println("There was an exception while reading the file: " + e.toString());
        }
        finally {

        }
    }
    public static void main(String[] args) {
        CustomMapMatching cmm = new CustomMapMatching();
        cmm.initiateGraphhoper();
        cmm.parseCsv("osm-dublin/edge_filtered.csv");
    }
}
