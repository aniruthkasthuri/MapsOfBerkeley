import java.util.HashMap;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 * <p>
 * The class will take as input an upper left latitude and longitude, a lower right
 * latitude and longitude,
 * a window width, and a window height. Using these six numbers, it will produce a
 * 2D array of filenames
 * corresponding to the files to be rendered. Historically, this has been the hardest
 * task to fully comprehend
 * and most time consuming part of the project.
 */
public class Rasterer {
    /**
     * imgRoot is the name of the directory containing the images.
     * You may not actually need this for your class.
     */
    static double[] bounds = new double[4];
    static double ROOT_ULLAT = MapServer.ROOT_ULLAT;
    static double ROOT_LRLAT = MapServer.ROOT_LRLAT;
    static double ROOT_ULLON = MapServer.ROOT_ULLON;
    static double ROOT_LRLON = MapServer.ROOT_LRLON;

    public static int depth(double ullat, double lrlat,
                            double lrlong, double ullong, double width) {
        double paramsPassed = boxLengthPerPixel((ullat + lrlat) / 2, lrlong, ullong, width);
        double depth = boxLengthPerPixel((37.892195547244356 + 37.82280243352756) / 2,
                -122.2998046875,
                -122.2119140625, 256.0);
        for (int count = 0; count < 7; count++) {
            if (depth < paramsPassed) {
                return count;
            }
            depth = depth / 2;
        }
        return 7;
    }

    /**
     * @Source: https://gis.stackexchange.com/questions/142326/calculating-longitude-length-in-miles
     * I used this source to help accurately calculate the
     * feet/pixel quantity as I personally felt that the constant
     * value given would be inaccurate in the long run
     */
    public static double boxLengthPerPixel(double lat, double lrlong, double ullong, double width) {
        double oneDegEq = 69.172;
        double longDiff = Math.abs(lrlong - ullong);
        // System.out.println("Diff "+longDiff);
        double latRad = Math.cos(Math.toRadians(lat));
        // System.out.println("LatRad "+latRad);
        return ((latRad * oneDegEq) / width) * longDiff * 5280;
    }

    public static String[][] imgName(double ullat, double lrlat,
                                     double lrlong, double ullong, double width) {
        double latChange = ROOT_ULLAT - ROOT_LRLAT;
        double lonChange = ROOT_LRLON - ROOT_ULLON;

        int depth = depth(ullat, lrlat, lrlong, ullong, width);

        latChange = latChange / Math.pow(2, depth);
        lonChange = lonChange / Math.pow(2, depth);
        //System.out.println("CHANGR: " + latChange);
        int minLat = minLat(lrlat, ROOT_ULLAT, latChange, depth);
        int maxLat = maxLat(ullat, ROOT_LRLAT, latChange, depth);
        int minLon = minLon(lrlong, ROOT_ULLON, lonChange, depth);
        int maxLon = maxLon(ullong, ROOT_LRLON, lonChange, depth);
        /*System.out.println(maxLat);
        System.out.println(minLat);
        System.out.println(maxLon);
        System.out.println(minLon);
        System.out.println("DEP: " + depth);*/

        String[][] imgs = new String[minLat - maxLat + 1][minLon - maxLon + 1];
        for (int x = 0; x <= minLat - maxLat; x++) {
            for (int y = 0; y <= minLon - maxLon; y++) {
                imgs[x][y] = "d" + depth + "_x" + (y + maxLon)
                        + "_y" + (x + maxLat) + ".png";
            }
        }
        return imgs;
    }

    private static int minLat(double lrlat, double endLat, double latChange, int depth) {
        double lat = endLat;
        if (lat <= lrlat) {
            bounds[0] = 37.892195547244356;
            return 0;
        }
        for (int i = 0; i < (int) Math.pow(2, depth); i++) {
            lat -= latChange;
            if (lat <= lrlat) {
                bounds[0] = lat;
                return i;
            }
        }
        bounds[0] = 37.82280243352756;
        return (int) (Math.pow(2, depth) - 1);
    }

    private static int maxLat(double ullat, double startLat, double latChange, int depth) {
        double lat = startLat;
        // System.out.println(ullat);
        if (lat >= ullat) {
            bounds[1] = MapServer.ROOT_LRLAT;
            return (int) (Math.pow(2, depth) - 1);
        }
        for (int i = (int) (Math.pow(2, depth) - 1); i > 0; i--) {
            lat += latChange;
            if (lat >= ullat) {
                //System.out.println(latChange);
                bounds[1] = lat;
                return i;
            }
        }
        bounds[1] = MapServer.ROOT_ULLAT;
        return 0;
    }

    private static int minLon(double lrlon, double endLon, double lonChange, int depth) {
        double lon = endLon;
        if (lon >= lrlon) {
            bounds[2] = -122.2998046875;
            return 0;
        }
        for (int i = 0; i < (int) (Math.pow(2, depth)); i++) {
            lon += lonChange;
            if (lon >= lrlon) {
                bounds[2] = lon;
                return i;
            }
        }
        bounds[2] = -122.2119140625;
        return (int) (Math.pow(2, depth) - 1);
    }

    private static int maxLon(double ullon, double startLon, double lonChange, int depth) {
        double lon = startLon;
        if (lon <= ullon) {
            bounds[3] = -122.2119140625;
            return (int) (Math.pow(2, depth) - 1);
        }
        for (int i = (int) (Math.pow(2, depth) - 1); i > 0; i--) {
            lon -= lonChange;
            if (lon <= ullon) {
                bounds[3] = lon;
                return i;
            }
        }
        bounds[3] = -122.2998046875;
        return 0;
    }


    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     * <p>
     * The grid of images must obey the following properties, where image in the
     * grid is referred to as a "tile".
     * <ul>
     * <li>The tiles collected must cover the most longitudinal distance per pixel
     * (LonDPP) possible, while still covering less than or equal to the amount of
     * longitudinal distance per pixel in the query box for the user viewport size. </li>
     * <li>Contains all tiles that intersect the query bounding box that fulfill the
     * above condition.</li>
     * <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     * </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * <<<<<<< HEAD
     * "depth"         : Number, the depth of the nodes of the rastered image;
     * can also be interpreted as the length of the numbers in the image
     * string. <br>
     * <<<<<<< HEAD
     * "query_success" -> Boolean, whether the query was able to successfully complete. Don't
     * forget to set this to true! <br>
     * @see #
     * =======
     * =======
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * >>>>>>> b2e1a2ec31a2679472630b686e13ab02c11802d3
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     * forget to set this to true on success! <br>
     * >>>>>>> 2de7d5773843cb2a7d40e2786af63c937f8fd109
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
       // System.out.println("YEEEEE" + params);
        Map<String, Object> results = new HashMap<>();
        results.put("depth", depth(params.get("ullat"), params.get("lrlat"),
                params.get("lrlon"), params.get("ullon"), params.get("w")));
        results.put("render_grid", imgName(params.get("ullat"), params.get("lrlat"),
                params.get("lrlon"), params.get("ullon"), params.get("w")));
        results.put("raster_lr_lat", bounds[0]);
        results.put("raster_ul_lat", bounds[1]);
        results.put("raster_lr_lon", bounds[2]);
        results.put("raster_ul_lon", bounds[3]);
        results.put("query_success", true);


        //System.out.println("Since you haven't implemented getMapRaster, nothing is displayed in "
        //      + "your browser.");
        return results;
    }
}
