import lbfgsb.LBFGSBException;
import lbfgsb.Minimizer;
import lbfgsb.Result;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by ghostof2007 on 5/24/14.
 * Expectation Maximization
 */
public class EM {

    HashMap<String, HashMap<String, Double>> word2Parent2logProb = new HashMap<String, HashMap<String, Double>>();
    //USE the same weights map as in Model2

    static double functionValue; //update every EStep - to be used by optimizer
    static HashMap<Integer, Double> featureGradients = new HashMap<Integer, Double>();

    static HashMap<String, ArrayList<String>> word2Neighbors = new HashMap<String, ArrayList<String>>();


    EM() {}

    static ArrayList<String> getNeighbors(String word) {

        if(word2Neighbors.containsKey(word))
            return word2Neighbors.get(word);

        //get a random subset of neighbors (maybe by transposing characters?)
        int NUM_NEIGHBORS = 5;
        ArrayList<String> neighbors = new ArrayList<String>();
        neighbors.add(word); //word is in its own neighbors set

        //generate neighbors
        HashSet<Integer> positions = new HashSet<Integer>();

        //prefix positions
        for(int i=0;i<NUM_NEIGHBORS;i++) {
            if(word.length() > i+1)
                positions.add(i);
            if(word.length() - i - 2 >=0 )
                positions.add(word.length()- i - 2);
        }

        for (int pos : positions) {
            //permute and add
            String newWord = word.substring(0, pos) + word.charAt(pos + 1) + word.charAt(pos);
            if (pos + 2 < word.length())
                newWord += word.substring(pos + 2);
            if (!newWord.equals(word))
                neighbors.add(newWord);
        }


        //TODO : make this parameterized
        if(word.length()>=4) {
            int n = word.length();
            if(word.length()>=4)
                neighbors.add(""+word.charAt(1)+word.charAt(0)+word.substring(2, n-2)+word.charAt(n-1)+word.charAt(n-2));
            if(word.length()>=5)
                neighbors.add(""+word.charAt(0)+word.charAt(2)+word.charAt(1)+word.substring(3, n-2)+word.charAt(n-1)+word.charAt(n-2));
            if(word.length()>=6)
                neighbors.add(""+word.charAt(0)+word.charAt(2)+word.charAt(1)+word.substring(3, n-3)+word.charAt(n-2)+word.charAt(n-3)+word.charAt(n-1));
            if(word.length()>=5)
                neighbors.add(""+word.charAt(1)+word.charAt(0)+word.substring(2, n-3)+word.charAt(n-2)+word.charAt(n-3)+word.charAt(n-1));
        }

        word2Neighbors.put(word, neighbors);

        return neighbors;
    }

    //calculate Z over the neighborhood of the word and update all the probabilities
    //also calculates the feature gradients
    static double calculateProbs(String word) {
        double val = 0.;

        //calculate for word first (hard EM_ON - choose only the best for the word)
        ArrayList<Pair<String, Integer>> candidates = MorphoChain.getCandidates(word, false);
        double logScore, bestScore = - Double.MAX_VALUE;
        HashMap<Integer, Double> bestFeatures = new HashMap<Integer, Double>();
        Pair<String, Integer> bestParent = null;
        for(Pair<String, Integer> parent : candidates) {
            HashMap<Integer, Double> features = MorphoChain.getFeatures(word, parent.getKey(), parent.getValue());
            logScore = Tools.featureWeightProduct(features);
            if(logScore > bestScore) {
                bestScore = logScore;
                bestParent = parent;
                bestFeatures = features;
            }
        }
        String myParent = bestParent.getKey();
        val += bestScore;
        Tools.updateMap(featureGradients, bestFeatures);

        //evaluate the neighbors
        ArrayList<String> neighbors = getNeighbors(word);
        ArrayList<Double> ZParts = new ArrayList<Double>();
        HashMap<Integer, Double> neighborhoodFeatures = new HashMap<Integer, Double>();
        for(String neighbor : neighbors) {
            candidates = MorphoChain.getCandidates(neighbor, false);
            for(Pair<String, Integer> parent : candidates) {
                HashMap<Integer, Double> features = MorphoChain.getFeatures(neighbor, parent.getKey(), parent.getValue());
                HashMap<String, Double> feature2Val = new HashMap<String, Double>();
                for(int featureID : features.keySet())
                    feature2Val.put(MorphoChain.index2Feature.get(featureID), features.get(featureID));
                logScore = Tools.featureWeightProduct(features);
                ZParts.add(logScore);
                Tools.updateMap(neighborhoodFeatures, features, Math.exp(logScore));
            }
        }
        double logZ = Tools.logSumOfExponentials(ZParts);

        assert !Double.isNaN(logZ);

        //update the gradients (static variable)
        Tools.updateMap(featureGradients, neighborhoodFeatures, -1./Math.exp(logZ));

        //update the function value
        functionValue += (val - logZ);

        return val - logZ;
    }

    //Soft EM version
    static double calculateProbsSoft(String word) {
        double val = 0.;

        //calculate for word first
        ArrayList<Pair<String, Integer>> candidates = MorphoChain.getCandidates(word, false);
        double logScore;
        ArrayList<Double> numeratorParts = new ArrayList<Double>();
        HashMap<Integer, Double> wordFeatures = new HashMap<Integer, Double>();
        for(Pair<String, Integer> parent : candidates) {
            HashMap<Integer, Double> features = MorphoChain.getFeatures(word, parent.getKey(), parent.getValue());
            logScore = Tools.featureWeightProduct(features);
            if(parent.getRight() == MorphoChain.STOP) logScore += Math.log(MorphoChain.STOP_FACTOR);
            numeratorParts.add(logScore);
            Tools.updateMap(wordFeatures, features, Math.exp(logScore));
        }
        val = Tools.logSumOfExponentials(numeratorParts);
        Tools.updateMap(featureGradients, wordFeatures, 1./Math.exp(val)); //IMP : have to sum in the soft EM_ON case

        //evaluate the neighbors
        ArrayList<String> neighbors = getNeighbors(word);
        ArrayList<Double> ZParts = new ArrayList<Double>();
        HashMap<Integer, Double> neighborhoodFeatures = new HashMap<Integer, Double>();
        for(String neighbor : neighbors) {
            candidates = MorphoChain.getCandidates(neighbor, false);
            for(Pair<String, Integer> parent : candidates) {
                HashMap<Integer, Double> features = MorphoChain.getFeatures(neighbor, parent.getKey(), parent.getValue());
                HashMap<String, Double> feature2Val = new HashMap<String, Double>();
                for(int featureID : features.keySet())
                    feature2Val.put(MorphoChain.index2Feature.get(featureID), MorphoChain.weights.get(featureID));
                logScore = Tools.featureWeightProduct(features);
                if(parent.getRight() == MorphoChain.STOP) logScore += Math.log(MorphoChain.STOP_FACTOR);
                ZParts.add(logScore);
                Tools.updateMap(neighborhoodFeatures, features, Math.exp(logScore)); //logScore is for this particular pair
            }
        }
        double logZ = Tools.logSumOfExponentials(ZParts);

        //update the gradients (static variable)
        Tools.updateMap(featureGradients, neighborhoodFeatures, -1./Math.exp(logZ));

        //for debug
        HashMap<String, Double> name2Gradient = new HashMap<String, Double>();
        HashMap<String, Double> name2Gradient2 = new HashMap<String, Double>();
        for(int key : wordFeatures.keySet())
            name2Gradient.put(MorphoChain.index2Feature.get(key), wordFeatures.get(key));

        for(int key : neighborhoodFeatures.keySet())
            name2Gradient2.put(MorphoChain.index2Feature.get(key), neighborhoodFeatures.get(key));


        //update the function value
        functionValue += (val - logZ);

        return val - logZ;
    }

    static void MStep() throws LBFGSBException {
        //clear all static variables
        functionValue = 0;
        featureGradients.clear();

        System.err.println("Starting EM_ON...");

        Minimizer alg = new Minimizer();
        alg.getStopConditions().setMaxIterations(500);
        alg.setDebugLevel(1);

        //copy the current weights to a double array and start the EM with that
        double [] weights = new double[MorphoChain.weights.size()];
        for(int i=0;i<weights.length;i++)
            weights[i] = MorphoChain.weights.get(i);


        Result ret = alg.run(new Function(), weights);
        double finalValue = ret.functionValue;
        double [] finalGradient = ret.gradient;
        System.out.println("EM finalValue : "+Double.toString(finalValue));
        System.out.println("EM finalGradientSum: "+Double.toString(Tools.squareSum(finalGradient)));


        //Store the final weights (for debugging)
        for(String feature : MorphoChain.feature2Index.keySet())
            MorphoChain.feature2Weight.put(feature, ret.point[MorphoChain.feature2Index.get(feature)]);
        //sort the weights
        MorphoChain.feature2Weight = Tools.sortByValue(MorphoChain.feature2Weight);
    }



}
