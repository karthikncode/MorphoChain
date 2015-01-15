import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Author: Karthik R Narasimhan
 *
 * Tools
 */
public class Tools {

    public static double max(double[] values) {
        double max = - Double.MAX_VALUE;
        for(double value : values) {
            if(value > max)
                max = value;
        }
        return max;
    }

    public static double logSumOfExponentials(double [] xs) {
        if (xs.length == 1) return xs[0];
        double max = max(xs);
        double sum = 0.0;
        for (double x : xs)
            if (x != Double.NEGATIVE_INFINITY)
                sum += Math.exp(x - max);
        return max + java.lang.Math.log(sum);
    }

    public static double logSumOfExponentials(ArrayList<Double> x) {
        double [] xs = new double[x.size()];
        for(int i=0;i<x.size(); i++)
            xs[i] = x.get(i);
        return logSumOfExponentials(xs);
    }

    static double dot(String a, String b) {
        double sum = 0.;
        ArrayList<Double> vec1 = MorphoChain.wordVec.get(a);
        ArrayList<Double> vec2 = MorphoChain.wordVec.get(b);
        if(vec1==null || vec2==null) return -0.5; //FIXME - make sure not using just plain DOT
        for(int i=0; i < vec1.size(); i++)
            sum += vec1.get(i) * vec2.get(i);
        return sum;
    }

    //dot product of feature and weights(global)
    static double featureWeightProduct(HashMap<Integer, Double> features) {
        double sum = 0.;
        if(features==null || features.size()==0) return 0.;
        for(int i : features.keySet())
            if( i < MorphoChain.weights.size())  //check if weight exists for the feature
                sum += features.get(i) * MorphoChain.weights.get(i);
        return sum;
    }

    //add values from one map to another (weighted by factor)
    static void updateMap(HashMap<Integer, Double> a, HashMap<Integer, Double> b, double factor) {
        for(int key : b.keySet()) {
            if (a.containsKey(key))
                a.put(key, a.get(key) + b.get(key) * factor);
            else
                a.put(key, b.get(key) * factor);
        }
    }

    static void updateMap(HashMap<Integer, Double> a, HashMap<Integer, Double> b) {
        updateMap(a, b, 1.);
    }

    static double sum(double [] array){
        double sum = 0.;
        for (double anArray : array) sum += anArray;
        return sum;
    }

    static double squareSum(double [] array){
        double sum = 0.;
        for (double anArray : array) sum += anArray * anArray;
        return sum;
    }

    static int getFeatureIndex(String  feature) {
        if(!MorphoChain.feature2Index.containsKey(feature)) {
            if(MorphoChain.TEST) return -1; //if in testing phase, and feature does not exist already, do not create new

            int index =  MorphoChain.feature2Index.size();
            MorphoChain.feature2Index.put(feature, index);
            MorphoChain.index2Feature.add(feature);
            MorphoChain.weights.add(0.);
            return  index;
        }
        return MorphoChain.feature2Index.get(feature);

    }

    //descending sort
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue( Map<K, V> map )
    {
        List<Map.Entry<K, V>> list =
                new LinkedList<Map.Entry<K, V>>( map.entrySet() );
        Collections.sort( list, new Comparator<Map.Entry<K, V>>()
        {
            public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )
            {
                return -(o1.getValue()).compareTo( o2.getValue() ); //change sign to make ascending
            }
        } );

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list)
        {
            result.put( entry.getKey(), entry.getValue() );
        }
        return result;
    }

    //sort map
    public static
    <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<T>(c);
        java.util.Collections.sort(list);
        return list;
    }

    static void addFeature(HashMap<Integer, Double> features, String newFeature, double value) {
        int featureIndex = getFeatureIndex(newFeature);
        if(featureIndex!=-1)
            features.put(featureIndex,value);
    }

    static void incrementMap(Map<Integer, Integer> map, int key) {
        Integer value = map.get(key);
        if(value==null)
            map.put(key,1);
        else
            map.put(key, value+1);
    }

    static void incrementMap(Map<String, Double> map, String key) {
        Double value = map.get(key);
        if(value==null)
            map.put(key,1.);
        else
            map.put(key, value+1);
    }

    public static double[] convertHashMap(HashMap<String, Double> map) {
        double [] values = new double[map.size()];
        int i =0;
        for(double val : map.values() )  {
            values[i] = val;
            i++;
        }
        return values;
    }

    public static HashMap<String, Double> normalizeHashMap(HashMap<String, Double> map) {
        HashMap<String, Double> newMap = new HashMap<String, Double>();
        double [] values = convertHashMap(map);
        double Z = sum(values);

        for(String key : map.keySet()) {
            assert !Double.isInfinite(map.get(key)) && !Double.isNaN(map.get(key));
            newMap.put(key, map.get(key)/Z);
        }
        return newMap;
    }

    static HashMap<String, Double> getFeatureNames(String word, String parent, int type) {
        HashMap<String, Double> features = new HashMap<String, Double>();
        for(Map.Entry<Integer, Double> entry : MorphoChain.getFeatures(word, parent, type).entrySet())
            features.put(MorphoChain.index2Feature.get(entry.getKey()), MorphoChain.weights.get(entry.getKey()));

        return features;
    }

    static void setFeatureWeight(String feature, double weight) {
        MorphoChain.weights.set(MorphoChain.feature2Index.get(feature), weight);
    }

    static HashSet<String> clone(HashSet<String> map) {
        HashSet<String> newMap = new HashSet<String>();
        for(String key : map)
            newMap.add(key);
        return newMap;
    }


    static HashMap<String, Map<String, Double>> computeAffixCorrelation(LinkedHashSet<String> affixes, char type) throws IOException {
        System.out.print("Computing affix correlation - " + type + " ...");
        String [] affixArray = new String[affixes.size()];
        affixArray = affixes.toArray(affixArray);
        double[][] correlationMatrix = new double[affixArray.length][affixArray.length];
        HashMap<String, HashSet<String>> affix2Word = new HashMap<String, HashSet<String>>();
        for(String affix : affixArray) {
            affix2Word.put(affix, new HashSet<String>());
            for(String word : MorphoChain.word2Cnt.keySet())
                if(type=='s' && word.endsWith(affix))
                    affix2Word.get(affix).add(word.substring(0,word.length()-affix.length()));
                else if(type=='p' && word.startsWith(affix))
                    affix2Word.get(affix).add(word.substring(affix.length()));
        }

        HashMap<String, Map<String, Double>> affixNeighbor = new HashMap<String, Map<String, Double>>();
        for(int i=0;i<affixArray.length;i++) {
            int bestJ = 0;
            double bestCorrelation = 0.;
            HashMap<String, Double> neighbor2Score = new HashMap<String, Double>();
            for (int j = 0; j < affixArray.length; j++)
                if (i != j) {
                    HashSet<String> tmp = Tools.clone(affix2Word.get(affixArray[i]));
                    tmp.retainAll(affix2Word.get(affixArray[j]));
                    correlationMatrix[i][j] = ((double) tmp.size()) / affix2Word.get(affixArray[i]).size();
                    neighbor2Score.put(affixArray[j], correlationMatrix[i][j]);
                    if(correlationMatrix[i][j] > bestCorrelation) {
                        bestCorrelation = correlationMatrix[i][j];
                        bestJ = j;
                    }
                }

            affixNeighbor.put(affixArray[i], Tools.sortByValue(neighbor2Score) );
        }

        System.out.println("done.");
        return affixNeighbor;
    }


    public static double entropy(ArrayList<Sample.MultinomialObject> multinomial) {
        double entropy = 0;
        for(Sample.MultinomialObject obj : multinomial) {
            entropy -= obj.score * Math.log(obj.score);
        }
        return entropy;
    }

    public static double maxValue(ArrayList<Sample.MultinomialObject> multinomial) {
        double maxVal = -Double.MAX_VALUE;
        for(Sample.MultinomialObject obj : multinomial) {
            if(obj.score > maxVal)
                maxVal = obj.score;
        }
        return maxVal;
    }
}
