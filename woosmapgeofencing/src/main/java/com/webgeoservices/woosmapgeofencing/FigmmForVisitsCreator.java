package com.webgeoservices.woosmapgeofencing;

import android.location.Location;
import android.util.Log;

import com.webgeoservices.woosmapgeofencing.database.Visit;
import com.webgeoservices.woosmapgeofencing.database.WoosmapDb;
import com.webgeoservices.woosmapgeofencing.database.ZOI;

import org.ojalgo.matrix.PrimitiveMatrix;
import org.ojalgo.matrix.decomposition.Eigenvalue;
import org.ojalgo.matrix.decomposition.EigenvalueDecomposition;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.PhysicalStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class FigmmForVisitsCreator {
    List<Map> list_zois = new ArrayList<>();
    List<Map> list_zois_to_delete = new ArrayList<>();
    int age_min = 5;
    int acc_min = 3;
    double chi_squared_value_for_update = chi_squared_value(0.95);
    Visit visit_m;

    WoosmapDb db_m;

    public FigmmForVisitsCreator(WoosmapDb db) {
        this.db_m = db;
    }

    public void figmmForVisit(Visit visit) {
        visit_m = visit;
        figmmAlgo(visit.lat,visit.lng,visit.accuracy);
    }

    public void figmmForLocation(Location position) {
        figmmAlgo(position.getLatitude(),position.getLongitude(),position.getAccuracy());
    }

    public void figmmAlgo(double xpos, double ypos, double accuracy) {
        //Get list of ZOI in DB
        getListOfZoi();

        double y = SphericalMercator.lat2y(xpos);
        double x = SphericalMercator.lon2x(ypos);
        MyPoint point = new MyPoint(x, y, accuracy);

        // Learning
        boolean zois_have_been_updated = incrementZOI(list_zois, point);

        // Creating new components
        if (!zois_have_been_updated) {
            createInitialCluster(point);
        }

        // Removing spurious components
        clean_clusters();

        // Update prior
        update_zois_prior();

        // Associate visit to zoi
        predict_as_dict(list_zois,point);

        // Update DB
        update_db();

    }

    private void update_db() {
        db_m.getZOIsDAO().deleteAllZOI();
        for (Iterator<Map> iter = list_zois.iterator(); iter.hasNext(); ) {
            Map<String, Object> zois_gmm_info = iter.next();
            ZOI newZOI = new ZOI();

            double[] mean_array = (double[]) zois_gmm_info.get("mean");
            newZOI.latMean = mean_array[0];
            newZOI.lngMean = mean_array[1];

            final PrimitiveMatrix covariance_matrix_inverse = (PrimitiveMatrix) zois_gmm_info.get("covariance_matrix_inverse");
            newZOI.x00Covariance_matrix_inverse = covariance_matrix_inverse.get(0);
            newZOI.x01Covariance_matrix_inverse = covariance_matrix_inverse.get(1);
            newZOI.x10Covariance_matrix_inverse = covariance_matrix_inverse.get(2);
            newZOI.x11Covariance_matrix_inverse = covariance_matrix_inverse.get(3);

            newZOI.accumulator = (Double) zois_gmm_info.get("accumulator");
            newZOI.age = (Double) zois_gmm_info.get("age");
            newZOI.covariance_det = (Double) zois_gmm_info.get("covariance_det");
            newZOI.prior_probability = (Double) zois_gmm_info.get("prior_probability");
            newZOI.wktPolygon = figmm(zois_gmm_info);
            newZOI.idVisits = (ArrayList<String>) zois_gmm_info.get("idVisits");

            long zoiDuration = 0;
            for (String visitUUIDZOI: newZOI.idVisits) {
                Visit visit = db_m.getVisitsDao().getVisitFromUuid(visitUUIDZOI);
                zoiDuration += visit.duration;

                if(newZOI.idVisits.get(0) == visitUUIDZOI) {
                    newZOI.startTime = visit.startTime;
                }

                if(newZOI.idVisits.get(newZOI.idVisits.size() - 1) == visitUUIDZOI) {
                    newZOI.endTime = visit.endTime;
                }

            }
            newZOI.duration = zoiDuration;


            db_m.getZOIsDAO().createZoi(newZOI);
        }


    }

    public double chi_squared_value(double probability) {
        return -2 * Math.log(1 - probability);
    }

    private void getListOfZoi() {
        ZOI[] zoiFromDB = db_m.getZOIsDAO().getAllZois();
        for (ZOI zoiDB : zoiFromDB) {
            Map<String, Object> zois_gmm_info = new HashMap<>();

            final double[][] myArray = {
                    {zoiDB.x00Covariance_matrix_inverse, zoiDB.x01Covariance_matrix_inverse}, {zoiDB.x10Covariance_matrix_inverse, zoiDB.x11Covariance_matrix_inverse}};

            final PrimitiveMatrix covariance_matrix_inverse = PrimitiveMatrix.FACTORY.rows(myArray);
            zois_gmm_info.put("covariance_matrix_inverse", covariance_matrix_inverse);

            zois_gmm_info.put("mean", new double[]{zoiDB.latMean,zoiDB.lngMean});

            zois_gmm_info.put("prior_probability", zoiDB.prior_probability);
            zois_gmm_info.put("age", zoiDB.age);
            zois_gmm_info.put("accumulator", zoiDB.accumulator);
            zois_gmm_info.put("updated", false);
            zois_gmm_info.put("covariance_det", zoiDB.covariance_det);
            zois_gmm_info.put("idVisits", zoiDB.idVisits);

            list_zois.add(zois_gmm_info);
        }
    }

    private void createInitialCluster(MyPoint newVisitPoint) {
        Log.d("WoosmapGeofencing", "createInitialCluster");
        // We use a multiplier because of true visit are not exactly on the position of the point.
        // So we left more variance to create clusters
        int covariance_multiplier = 2;
        double sigma = newVisitPoint.getAccuray() * covariance_multiplier;
        double covariance_initial_value = Math.pow(sigma, 2);

        Map<String, Object> zois_gmm_info = new HashMap<>();

        zois_gmm_info.put("mean", new double[]{newVisitPoint.getX(), newVisitPoint.getY()});

        final double[][] covariance_matrix_inverse_Array = {
                {1.0, 0.0},
                {0.0, 1.0}
        };
        final PrimitiveMatrix covariance_matrix_inverse = PrimitiveMatrix.FACTORY.rows(covariance_matrix_inverse_Array).multiply(Math.pow(sigma, -2));
        zois_gmm_info.put("covariance_matrix_inverse", covariance_matrix_inverse);

        if (list_zois.isEmpty()) {
            zois_gmm_info.put("prior_probability", 1.0);
        } else {
            double prior_probability = 1.0;
            for (Iterator<Map> iter = list_zois.iterator(); iter.hasNext(); ) {
                Map<String, Object> gmm_info = iter.next();
                prior_probability += (double) gmm_info.get("accumulator");
            }
            zois_gmm_info.put("prior_probability", 1/prior_probability);
        }

        zois_gmm_info.put("age", 1.0);
        zois_gmm_info.put("accumulator", 1.0);
        zois_gmm_info.put("updated", true);
        zois_gmm_info.put("covariance_det", Math.pow(covariance_initial_value, 2));

        List<String> idVisit = new ArrayList<>();
        zois_gmm_info.put("idVisits",idVisit);
        zois_gmm_info.put("startTime",visit_m.startTime);
        zois_gmm_info.put("endTime",visit_m.endTime);


        list_zois.add(zois_gmm_info);
    }

    private boolean incrementZOI(List<Map> list_zois_gmm_info, MyPoint visitPoint) {
        Log.d("WoosmapGeofencing", "incrementZOI");
        boolean zois_have_been_updated = false;
        List<Double> cov_determinants = new ArrayList<>();
        List<Double> prior_probabilities = new ArrayList<>();
        List<Double> sqr_mahalanobis_distances = new ArrayList<>();
        MyPoint point = new MyPoint(visitPoint.getX(), visitPoint.getY());

        for (Iterator<Map> iter = list_zois_gmm_info.iterator(); iter.hasNext(); ) {
            Map<String, Object> zois_gmm_info = iter.next();
            cov_determinants.add((Double) zois_gmm_info.get("covariance_det"));
            prior_probabilities.add((Double) zois_gmm_info.get("prior_probability"));

            double[][] point_array = {{visitPoint.getX(), visitPoint.getY()},
                    {0.0,0.0}};
            double[] mean_arrayFromZOI = (double[]) zois_gmm_info.get("mean");
            double[][] mean_array = {{mean_arrayFromZOI[0],mean_arrayFromZOI[1]},
                    {0.0,0.0}};

            final PrimitiveMatrix matrix_point = PrimitiveMatrix.FACTORY.rows(point_array);
            final PrimitiveMatrix matrix_mean = PrimitiveMatrix.FACTORY.rows(mean_array);
            final PrimitiveMatrix matrix_error = matrix_point.subtract(matrix_mean);

            final PrimitiveMatrix covariance_matrix_inverse = (PrimitiveMatrix) zois_gmm_info.get("covariance_matrix_inverse");
            final PrimitiveMatrix a = matrix_error.multiplyLeft(covariance_matrix_inverse);
            PrimitiveMatrix a2 = a.multiplyRight(matrix_error.transpose());

            double mahalanobis_distance = Math.sqrt(a2.get(0));
            sqr_mahalanobis_distances.add(Math.pow(mahalanobis_distance, 2));

        }

        // We calculate all values at once using matrix calculations
        double[] x_j_probabilities = get_probability_of_x_knowing_cluster(cov_determinants, sqr_mahalanobis_distances);
        double[] result_x_j_prob_prior_prob_Array = new double[x_j_probabilities.length];


        for (int i = 0; i < x_j_probabilities.length; i++) {
            result_x_j_prob_prior_prob_Array[i] = x_j_probabilities[i] * prior_probabilities.get(i);
        }


        final PrimitiveMatrix result_x_j_prob_prior_prob_Matrix = PrimitiveMatrix.FACTORY.rows(result_x_j_prob_prior_prob_Array);

        double normalization_coefficient = 0.0;
        for (int i = 0; i < result_x_j_prob_prior_prob_Array.length; i++) {
            normalization_coefficient += result_x_j_prob_prior_prob_Array[i];
        }

        // Update all clusters close to the visit
        for (int i = 0; i < list_zois_gmm_info.size(); i++) {
            if (sqr_mahalanobis_distances.get(i) <= chi_squared_value_for_update) {
                update_cluster(point, x_j_probabilities[i], list_zois_gmm_info.get(i), normalization_coefficient);
                zois_have_been_updated = true;
            }
        }

        return zois_have_been_updated;

    }

    public double[] get_probability_of_x_knowing_cluster(List<Double> cov_determinants, List<Double> sqr_mahalanobis_distances) {
        Log.d("WoosmapGeofencing", "get_probability_of_x_knowing_cluster");
        List<Double> cov_determinants_to_evaluate = new ArrayList<>(cov_determinants);
        List<Double> sqr_mahalanobis_distances_to_evaluate = new ArrayList<>(sqr_mahalanobis_distances);
        ListIterator<Double> iterator = sqr_mahalanobis_distances_to_evaluate.listIterator();
        while (iterator.hasNext()) {
            Double next = iterator.next();
            iterator.set(Math.exp(-next / 2));
        }

        ListIterator<Double> iterator2 = cov_determinants_to_evaluate.listIterator();
        while (iterator2.hasNext()) {
            Double next = iterator2.next();
            iterator2.set(Math.sqrt(next) * 2 * Math.PI);
        }

        double[] probability_of_x_knowing_cluster = new double[cov_determinants_to_evaluate.size()];

        for (int i = 0; i < cov_determinants_to_evaluate.size(); i++) {
            probability_of_x_knowing_cluster[i] = sqr_mahalanobis_distances_to_evaluate.get(i) / cov_determinants_to_evaluate.get(i);
        }


        return probability_of_x_knowing_cluster;
    }

    public void update_cluster(MyPoint point, double x_j_probability, Map zoi_gmminfo, double normalization_coefficient) {
        Log.d("WoosmapGeofencing", "update_cluster");
        double j_x_probability = x_j_probability * (Double) zoi_gmminfo.get("prior_probability") / normalization_coefficient;

        double[][] point_array = {{point.getX(), point.getY()},
                {0.0,0.0}};
        double[] mean_arrayFromZOI = (double[]) zoi_gmminfo.get("mean");
        double[][] mean_array = {{mean_arrayFromZOI[0],mean_arrayFromZOI[1]},
                {0.0,0.0}};

        zoi_gmminfo.put("age", (double) zoi_gmminfo.get("age") + 1.0);
        //Log("normalization_coefficient", String.valueOf(normalization_coefficient));
        //Log("j_x_probability", String.valueOf(j_x_probability));
        zoi_gmminfo.put("accumulator", (Double) zoi_gmminfo.get("accumulator") + j_x_probability);


        final PrimitiveMatrix matrix_point = PrimitiveMatrix.FACTORY.rows(point_array);
        final PrimitiveMatrix matrix_mean = PrimitiveMatrix.FACTORY.rows(mean_array);
        final PrimitiveMatrix matrix_error = matrix_point.subtract(matrix_mean);
        double weight = j_x_probability / (Double) zoi_gmminfo.get("accumulator");
        final PrimitiveMatrix matrix_delta_mean = matrix_error.multiply(weight);
        final PrimitiveMatrix matrix_mean_plus_delta_mean = matrix_delta_mean.add(matrix_mean);
        final PrimitiveMatrix covariance_matrix_inverse = (PrimitiveMatrix) zoi_gmminfo.get("covariance_matrix_inverse");
        final PrimitiveMatrix matrix_new_error = matrix_point.subtract(matrix_mean_plus_delta_mean);


        double factorTerm1 = weight / Math.pow((1 - weight), 2);
        final PrimitiveMatrix matrix_new_term1a = covariance_matrix_inverse.multiplyLeft(matrix_new_error.transpose());
        final PrimitiveMatrix matrix_new_term1b = matrix_new_term1a.multiplyRight(matrix_new_error);
        final PrimitiveMatrix matrix_new_term1c = matrix_new_term1b.multiplyRight(covariance_matrix_inverse);
        final PrimitiveMatrix matrix_new_term1 = matrix_new_term1c.multiply(factorTerm1);

        double factor = weight / (1 - weight);
        final PrimitiveMatrix matrix_new_term2a = matrix_new_error.multiply(factor);
        final PrimitiveMatrix matrix_new_term2b = matrix_new_term2a.multiplyLeft(covariance_matrix_inverse);
        final PrimitiveMatrix matrix_new_errorTransposed = matrix_new_error.transpose();
        final PrimitiveMatrix matrix_new_term2 = matrix_new_term2b.multiplyRight(matrix_new_errorTransposed).add(1);

        final PrimitiveMatrix cov_inv_delta1 = covariance_matrix_inverse.divide((1 - weight));

        final PrimitiveMatrix cov_inv_delta2_matrix = matrix_new_term1.divide(matrix_new_term2.get(0));
        final PrimitiveMatrix cov_inv_delta_matrix = cov_inv_delta1.subtract(cov_inv_delta2_matrix);


        final PrimitiveMatrix matrix_new_term3 = cov_inv_delta_matrix
                .multiplyRight(matrix_delta_mean.transpose())
                .multiplyRight(matrix_delta_mean)
                .multiplyRight(cov_inv_delta_matrix);

        final PrimitiveMatrix matrix_new_term4a = matrix_delta_mean
                .multiplyRight(cov_inv_delta_matrix)
                .multiplyRight(matrix_delta_mean.transpose());

        double term4 = 1 - matrix_new_term4a.get(0);
        final PrimitiveMatrix new_inv_matrix = cov_inv_delta_matrix.add(matrix_new_term3.divide(term4));

        double cov_det_delta1 = Math.pow(1 - weight, 2) * ((Double) zoi_gmminfo.get("covariance_det"));
        final PrimitiveMatrix cov_det_delta2 = matrix_new_error.multiplyRight(cov_inv_delta_matrix).multiplyRight(matrix_new_error.transpose());
        final PrimitiveMatrix cov_det_delta3 = cov_det_delta2.multiply(weight / (1 - weight)).add(1);
        double cov_det_delta = cov_det_delta1 * cov_det_delta3.get(0);

        final PrimitiveMatrix new_covariance_determinant1 = matrix_delta_mean.multiplyRight(cov_inv_delta_matrix).multiplyRight(matrix_delta_mean.transpose());
        double new_covariance_determinant = cov_det_delta * (1 - new_covariance_determinant1.get(0));


        zoi_gmminfo.put("updated", true);
        zoi_gmminfo.put("mean", new double[]{matrix_mean_plus_delta_mean.get(0,0), matrix_mean_plus_delta_mean.get(0,1)});
        double[] mean_array2 = (double[]) zoi_gmminfo.get("mean");
        System.out.println("zoi_gmminfo.put(mean) = " + mean_array2[0] + " " + mean_array2[1]);

        if (new_covariance_determinant > 0) {
            zoi_gmminfo.put("covariance_matrix_inverse", new_inv_matrix);
            zoi_gmminfo.put("covariance_det", new_covariance_determinant);
        }

    }

    private void clean_clusters() {
        Log.d("WoosmapGeofencing", "clean_clusters");
        for (Iterator<Map> iter = list_zois.iterator(); iter.hasNext(); ) {
            Map<String, Object> zois_gmm_info = iter.next();
            if ((double) zois_gmm_info.get("age") > age_min && (double) zois_gmm_info.get("accumulator") < acc_min) {
                list_zois_to_delete.add(zois_gmm_info);
            }
        }
    }

    private void update_zois_prior() {
        Log.d("WoosmapGeofencing", "update_zois_prior");
        double normalization_params = 0.0;
        for (Iterator<Map> iter = list_zois.iterator(); iter.hasNext(); ) {
            Map<String, Object> zois_gmm_info = iter.next();
            normalization_params += (double) zois_gmm_info.get("accumulator");

        }

        for (Iterator<Map> iter = list_zois.iterator(); iter.hasNext(); ) {
            Map<String, Object> zois_gmm_info = iter.next();
            zois_gmm_info.put("prior_probability", (double) zois_gmm_info.get("accumulator") / normalization_params);

        }

    }

    public String figmm(Map zoi_gmminfo) {

        final PrimitiveMatrix covariance_matrix_inverse = (PrimitiveMatrix) zoi_gmminfo.get("covariance_matrix_inverse");
        final PrimitiveMatrix covariance_matrix = covariance_matrix_inverse.invert();

        final Eigenvalue<Double> evd = EigenvalueDecomposition.makePrimitive();
        evd.compute(covariance_matrix);
        final MatrixStore<Double> tmpV = evd.getV();
        final PhysicalStore<Double> tmpD = evd.getD().copy();

        Double[] vectorValue = {0.0,0.0};
        if(tmpD.get(0) > tmpD.get(3)) {
            vectorValue[0] = tmpV.get(0);
            vectorValue[1] = tmpV.get(1);
        } else {
            vectorValue[0] = tmpV.get(2);
            vectorValue[1] = tmpV.get(3);
        }

        double A_norm = Math.sqrt(Math.pow(vectorValue[0], 2) + Math.pow(vectorValue[1], 2));

        double cos_theta = vectorValue[0] / A_norm;
        double sin_theta = vectorValue[1] / A_norm;
        double[][] a_vector ={
                {cos_theta, sin_theta},
                {0.0,0.0}};
        double[][] b_vector ={
                {-sin_theta, cos_theta},
                {0.0,0.0}};

        double[] mean_array = (double[]) zoi_gmminfo.get("mean");
        double x_mean = mean_array[0];
        double y_mean = mean_array[1];

        final PrimitiveMatrix a_vector_Matrix = PrimitiveMatrix.FACTORY.rows(a_vector);
        final PrimitiveMatrix a1 = covariance_matrix_inverse.multiplyLeft(a_vector_Matrix);
        final PrimitiveMatrix a2 = a_vector_Matrix.transpose();
        final PrimitiveMatrix a = a1.multiplyRight(a2);

        double a_val = Math.sqrt(chi_squared_value(0.7) / a.doubleValue(0));

        final PrimitiveMatrix b_vector_Matrix = PrimitiveMatrix.FACTORY.rows(b_vector);
        final PrimitiveMatrix b = b_vector_Matrix.multiplyLeft(covariance_matrix_inverse).multiplyRight(b_vector_Matrix.transpose());

        double b_val = Math.sqrt(chi_squared_value(0.7) / b.doubleValue(0));

        StringBuilder sb = new StringBuilder();
        sb.append("POLYGON((");
        int step = 8;
        int valLimit = (int) (2 * Math.PI * step) + 1;

        MyPoint[] ellipse_points = new MyPoint[valLimit + 1];
        for (double i = 0; i < valLimit; i++) {
            double t = i / step;
            double cos_t = Math.cos(t);
            double sin_t = Math.sin(t);
            double x = x_mean + a_val * cos_theta * cos_t - b_val * sin_theta * sin_t;
            double y = y_mean + a_val * sin_theta * cos_t + b_val * cos_theta * sin_t;

            sb.append(SphericalMercator.x2lon(x));
            sb.append(" ");
            sb.append(SphericalMercator.y2lat(y));
            if (i + 1 < ((2 * Math.PI * step)))
                sb.append(",");
            ellipse_points[(int) i] = new MyPoint(x, y);

        }

        ellipse_points[valLimit] = ellipse_points[0];
        sb.append("))");

        return sb.toString();
    }

    //predict cluster for each data and return them as dict to optimized insertion
    public void predict_as_dict(List<Map> list_zois_gmm_info, MyPoint visitPoint) {
        List<Double> cov_determinants = new ArrayList<>();
        List<Double> prior_probabilities = new ArrayList<>();
        List<Double> sqr_mahalanobis_distances = new ArrayList<>();

        for (Iterator<Map> iter = list_zois_gmm_info.iterator(); iter.hasNext(); ) {
            Map<String, Object> zois_gmm_info = iter.next();
            cov_determinants.add((Double) zois_gmm_info.get("covariance_det"));
            prior_probabilities.add((Double) zois_gmm_info.get("prior_probability"));
            double[][] point_array = {{visitPoint.getX(), visitPoint.getY()},
                    {0.0,0.0}};
            double[] mean_arrayFromZOI = (double[]) zois_gmm_info.get("mean");
            double[][] mean_array = {{mean_arrayFromZOI[0],mean_arrayFromZOI[1]},
                    {0.0,0.0}};

            final PrimitiveMatrix matrix_point = PrimitiveMatrix.FACTORY.rows(point_array);
            final PrimitiveMatrix matrix_mean = PrimitiveMatrix.FACTORY.rows(mean_array);
            final PrimitiveMatrix matrix_error = matrix_point.subtract(matrix_mean);

            final PrimitiveMatrix covariance_matrix_inverse = (PrimitiveMatrix) zois_gmm_info.get("covariance_matrix_inverse");
            final PrimitiveMatrix a = matrix_error.multiplyLeft(covariance_matrix_inverse);
            PrimitiveMatrix a2 = a.multiplyRight(matrix_error.transpose());

            double mahalanobis_distance = Math.sqrt((Double) a2.get(0));
            sqr_mahalanobis_distances.add(Math.pow(mahalanobis_distance, 2));

        }

        // We calculate all values at once using matrix calculations
        double[] x_j_probabilities = get_probability_of_x_knowing_cluster(cov_determinants, sqr_mahalanobis_distances);
        double[] result_x_j_prob_prior_prob_Array = new double[x_j_probabilities.length];


        for (int i = 0; i < x_j_probabilities.length; i++) {
            result_x_j_prob_prior_prob_Array[i] = x_j_probabilities[i] * prior_probabilities.get(i);
        }

        int index = 0;
        double max = result_x_j_prob_prior_prob_Array[0];

        for (int i = 0; i < result_x_j_prob_prior_prob_Array.length; i++)
        {
            if (max < result_x_j_prob_prior_prob_Array[i])
            {
                max = result_x_j_prob_prior_prob_Array[i];
                index = i;
            }
        }

        List<String> idVisits = (List<String>) list_zois_gmm_info.get(index).get("idVisits");
        if(idVisits == null) {
            idVisits = new ArrayList<>();
        }
        idVisits.add((String) visit_m.uuid);
        list_zois_gmm_info.get(index).put("idVisits",idVisits);

    }

}


class MyPoint {

    private double x;
    private double y;
    private double accuracy;


    public MyPoint(double x, double y) {
        this.x = x;
        this.y = y;
        this.accuracy = 20.0;
    }

    public MyPoint(double x, double y, double accuracy) {
        this.x = x;
        this.y = y;
        this.accuracy = accuracy;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getAccuray() {
        return accuracy;
    }

}

class SphericalMercator {
    public static final double RADIUS = 6378137.0; /* in meters on the equator */

    /* These functions take their length parameter in meters and return an angle in degrees */

    public static double y2lat(double aY) {
        return Math.toDegrees(Math.atan(Math.exp(aY / RADIUS)) * 2 - Math.PI/2);
    }
    public static double x2lon(double aX) {
        return Math.toDegrees(aX / RADIUS);
    }

    /* These functions take their angle parameter in degrees and return a length in meters */

    public static double lat2y(double aLat) {
        return Math.log(Math.tan(Math.PI / 4 + Math.toRadians(aLat) / 2)) * RADIUS;
    }
    public static double lon2x(double aLong) {
        return Math.toRadians(aLong) * RADIUS;
    }
}