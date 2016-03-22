import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by ghostof2007 on 5/6/14.
 *
 * Model2 file
 */
public class MorphoChain {
    //constants - assigned to by params.properties file
    static int FREQ_THRESHOLD;
    static int VECTOR_SIZE = 200;
    static double LAMBDA = 1; //OLD: not used
    static double LAMBDA_EM; //lambda for objective regularization
    static int HEURISTIC_FREQ_THRESHOLD;
    static int AFFIX_FREQ_THRESHOLD;
    static int MIN_SEG_LENGTH;
    static int TOP_AFFIX_SELECT;
    static int MAX_AFFIX_LENGTH = 4;
    static int TOP_AFFIX_NEIGHBORS = 1;

    //conditions for features/run types
    static final boolean REPEAT_ON = true;
    static final boolean MODIFY_ON = false;
    static final boolean DELETE_ON = true;
    static final boolean INDUCTIVE = true;
    static final boolean DOT = true;
    static boolean AFFIX_NEIGHBORS= true;

    //IMP : unused
    static boolean TEST = false; //turn on while testing to avoid creating new features - don't change manually
    static boolean EM_ON;  //controlled by Main
    static boolean AFFIX_CONTEXT = false; //NOT for neighbors
    static boolean DEBUG = false;


    //data files (overridden by params!)
    static String wordVectorFile;
    static String wordListFile;
    static String testListFile;
    static String goldSegFile;

    //enum constants
    static int STOP = 1;
    static int REPEAT = 2;
    static int MODIFY = 3;
    static int SUFFIX = 4;
    static int PREFIX = 5;
    static int COMPOUND = 6;
    static int DELETE = 7;

    //param
    static double STOP_FACTOR = 1;

    //language specific
    static String ALPHABET="qwertyuiopasdfghjklzxcvbnm";


    static HashMap<String, ArrayList<Double>> wordVec = new HashMap<String, ArrayList<Double>>();
    static HashMap<String, Integer> word2Cnt = new HashMap<String, Integer>();
    static ArrayList<Pair<String, ArrayList<String>>> goldSegs = new ArrayList<Pair<String, ArrayList<String>>>();
    static HashMap<String, ArrayList<String>> goldSegmentations = new HashMap<String, ArrayList<String>>();
    static HashMap<String, Integer> feature2Index = new HashMap<String, Integer>();
    static ArrayList<String> index2Feature = new ArrayList<String>();
    static ArrayList<Double> weights = new ArrayList<Double>();
    static HashMap<String, Double> word2MaxDot  = new HashMap<String, Double>();

    //for producing segmentations
    static HashMap<String, ArrayList<String>> testList = new HashMap<String, ArrayList<String>>();

    //for debugging
    static Map<String, Double> feature2Weight = new HashMap<String, Double>();

    //affixes
    static LinkedHashSet<String> prefixes = new LinkedHashSet<String>();
    static LinkedHashSet<String> suffixes = new LinkedHashSet<String>();
    static HashMap<String, Map<String, Double>> suffixNeighbor = new HashMap<String, Map<String, Double>>();
    static HashMap<String, Map<String, Double>> prefixNeighbor = new HashMap<String, Map<String, Double>>();
    static HashMap<String, Double> suffixDist = new HashMap<String, Double>();
    static HashMap<String, Double> prefixDist = new HashMap<String, Double>();

    //caching of features
    static HashMap<String, HashMap<String, HashMap<Integer, HashMap<Integer, Double>>>> w2P2TypeFeatures
            = new HashMap<String, HashMap<String, HashMap<Integer, HashMap<Integer, Double>>>>();

    Function func;

    void readWordVectors() throws IOException, InterruptedException {
        BufferedReader br = new BufferedReader(new FileReader(wordVectorFile));
        try {
            StringBuilder sb;
            String line = br.readLine();
            while (line != null) {
                sb = new StringBuilder();
                sb.append(line);
                String[] parts = sb.toString().split(" ");
                ArrayList<Double> vector = new ArrayList<Double>();
                String word = parts[0];
                for(int i=1;i<Math.min(VECTOR_SIZE + 1, parts.length);i++) {
                    vector.add(Double.parseDouble(parts[i]));
                }
                wordVec.put(word, vector);
                line = br.readLine();
            }
        } finally {
            br.close();
        }
        System.err.println("Read in "+Integer.toString(wordVec.size())+" vectors");
    }

    void readWordList() throws IOException, InterruptedException {
        BufferedReader br = new BufferedReader(new FileReader(wordListFile));
        try {
            StringBuilder sb;
            String line = br.readLine();

            while (line != null) {
                sb = new StringBuilder();
                sb.append(line);
                String[] parts = sb.toString().split(" ");
                String word = parts[0];
                if(word.length()>=MIN_SEG_LENGTH)
                    word2Cnt.put(word, Integer.parseInt(parts[1]));
                line = br.readLine();
            }
        } finally {
            br.close();
        }
        System.err.println("Read in "+Integer.toString(word2Cnt.size())+" words");
    }

    void readGoldSegmentations() throws IOException, InterruptedException {
        BufferedReader br = new BufferedReader(new FileReader(goldSegFile));
        try {
            StringBuilder sb;
            String line = br.readLine();

            while (line != null) {
                sb = new StringBuilder();
                sb.append(line);
                String[] parts = sb.toString().split("[: ]");
                String word = parts[0];
                ArrayList<String> segmentations = new ArrayList<String>();
                for(int i=1;i<parts.length; i++)
                    segmentations.add(parts[i]);
                goldSegmentations.put(word, segmentations);
                goldSegs.add(new MutablePair<String, ArrayList<String>>(word, segmentations));

                //inductive mode
                if(INDUCTIVE && !word2Cnt.containsKey(word))
                    word2Cnt.put(word, MorphoChain.FREQ_THRESHOLD+1);


                line = br.readLine();
            }
        } finally {
            br.close();
        }
        System.err.println("Read in "+goldSegmentations.size()+" segmentations.");
    }

    MorphoChain() {}

    void initialize() throws IOException, InterruptedException {
        readWordVectors();
        readWordList();
        readGoldSegmentations();

        selectMostFrequentAffixes();

        //calculate all possible features and update the weights vector
        func = new Function();
        int i = 0;

        System.err.println("Initializing features....");
        for(String word : MorphoChain.word2Cnt.keySet()) {
            if(MorphoChain.word2Cnt.get(word) < FREQ_THRESHOLD) continue;

            for(String neighbor : EM.getNeighbors(word)) {
                for(Pair<String, Integer> parentAndType : getCandidates(neighbor)) {
                    getFeatures(neighbor, parentAndType.getKey(), parentAndType.getValue());
                }
            }
            System.err.print("\r"+(i++));
        }
        System.err.println();

    }

    // -------------------------------------- Features ------------------------------------------------

    //Get the feature for word-parent pair
    static HashMap<Integer, Double> getFeatures(String word, String parent, int type) {
        //check feature cache
        if(checkCacheExists(word, parent, type))
            return  w2P2TypeFeatures.get(word).get(parent).get(type);


        //stop features
        if(type == STOP || parent.equals(word)) {
             HashMap<Integer, Double> stopFeatures = getStopFeatures(word);
             cacheFeatures(word, parent, type, stopFeatures);
             return stopFeatures;
         }


        HashMap<Integer, Double> features = new HashMap<Integer, Double>();

        //DOT
        double cosine = Tools.dot(word, parent);

        //maxDot caching
        if(!word2MaxDot.containsKey(word))
            word2MaxDot.put(word, cosine);
        else {
            double maxDotOld = word2MaxDot.get(word);
            if (cosine > maxDotOld)
                word2MaxDot.put(word, cosine);
        }

        if(DOT)
            Tools.addFeature(features, "DOT", cosine);

        //affix
        String affix = "";
        String inVocab = "";

        if(word2Cnt.containsKey(parent) && word2Cnt.get(parent) > HEURISTIC_FREQ_THRESHOLD) {
            Tools.addFeature(features, "_IV_", Math.log(word2Cnt.get(parent)));
        }
        else
            Tools.addFeature(features, "_OOV_", 1.);


        if(type == SUFFIX) {
            //suffix case
            affix = word.substring(parent.length());
            if (affix.length() > MAX_AFFIX_LENGTH || !suffixes.contains(affix)) affix = "UNK";
            if(!affix.equals("UNK")) {
                Tools.addFeature(features, inVocab + "SUFFIX_" + affix, 1.);
            }

            if(AFFIX_NEIGHBORS && suffixNeighbor.containsKey(affix)) {
                int i = 0;
                for(String neighbor : suffixNeighbor.get(affix).keySet()) {
                    if (word2Cnt.containsKey(parent + neighbor)) {
                        Tools.addFeature(features, "COR_S_" + affix, 1.);
                        break;
                    }
                    i++;
                    if(i==TOP_AFFIX_NEIGHBORS) break;
                }
            }


            //context features
            if(AFFIX_CONTEXT) {
                Tools.addFeature(features, inVocab + "SUFFIX_" + affix + "_BOUNDARY_" + parent.substring(parent.length() - 1), 1.);

                if (parent.length() >= 2) {
                    Tools.addFeature(features, inVocab + "SUFFIX_" + affix + "_BOUNDARY_" + parent.substring(parent.length() - 2), 1.);
                }
            }
        }
        else if (type == REPEAT) {
            //assuming affix is only on the right side
            affix = word.substring(parent.length()+1);
            if (!suffixes.contains(affix)) affix = "UNK";
            if(!affix.equals("UNK")) {
                Tools.addFeature(features, inVocab + "SUFFIX_" + affix, 1.);
            }

            if(AFFIX_NEIGHBORS && suffixNeighbor.containsKey(affix)) {
                int i = 0;
                for(String neighbor : suffixNeighbor.get(affix).keySet()) {
                    if (word2Cnt.containsKey(parent + neighbor)) {
                        Tools.addFeature(features, "COR_S_" + affix, 1.);
                        break;
                    }
                    i++;
                    if(i==TOP_AFFIX_NEIGHBORS) break;
                }
            }

            //context features
            if(AFFIX_CONTEXT) {
                Tools.addFeature(features, inVocab + "SUFFIX_" + affix + "_BOUNDARY_" + parent.substring(parent.length() - 1), 1.);
                if (parent.length() >= 2)
                    Tools.addFeature(features, inVocab + "SUFFIX_" + affix + "_BOUNDARY_" + parent.substring(parent.length() - 2), 1.);
            }
            //REPEAT specific features
            int parentLen = parent.length();
            Tools.addFeature(features, inVocab+"REPEAT_"+ word.charAt(parentLen), 1.);

        }
        else if (type == MODIFY) { //change last letter of parent
            //assuming affix is only on the right side
            affix = word.substring(parent.length());
            if (!suffixes.contains(affix)) affix = "UNK";
            if(!affix.equals("UNK"))
                Tools.addFeature(features, inVocab + "SUFFIX_" + affix, 1.);

            if(AFFIX_NEIGHBORS && suffixNeighbor.containsKey(affix)) {
                int i = 0;
                for(String neighbor : suffixNeighbor.get(affix).keySet()) {
                    if (word2Cnt.containsKey(parent + neighbor)) {
                        Tools.addFeature(features, "COR_S_" + affix, 1.);
                        break;
                    }
                    i++;
                    if(i==TOP_AFFIX_NEIGHBORS) break;
                }
            }

            //context features
            if(AFFIX_CONTEXT) {
                Tools.addFeature(features, inVocab + "SUFFIX_" + affix + "_BOUNDARY_" + parent.substring(parent.length() - 1), 1.);
                if (parent.length() >= 2)
                    Tools.addFeature(features, inVocab + "SUFFIX_" + affix + "_BOUNDARY_" + parent.substring(parent.length() - 2), 1.);
            }

            //MODIFY specific features
            int parentLen = parent.length();
            Tools.addFeature(features, inVocab+"MODIFY_"+parent.charAt(parentLen - 1)+"_"+word.charAt(parentLen - 1), 1.);
        }

        else if (type == DELETE) { //add last letter of parent
            //assuming affix is only on the right side
            affix = word.substring(parent.length());
            if (!suffixes.contains(affix)) affix = "UNK";
            if(!affix.equals("UNK"))
                Tools.addFeature(features, inVocab + "SUFFIX_" + affix, 1.);

            if(AFFIX_NEIGHBORS && suffixNeighbor.containsKey(affix)) {
                int i = 0;
                for(String neighbor : suffixNeighbor.get(affix).keySet()) {
                    if (word2Cnt.containsKey(parent + neighbor)) {
                        Tools.addFeature(features, "COR_S_" + affix, 1.);
                        break;
                    }
                    i++;
                    if(i==TOP_AFFIX_NEIGHBORS) break;
                }
            }

            //context features
            if(AFFIX_CONTEXT) {
                Tools.addFeature(features, inVocab + "SUFFIX_" + affix + "_BOUNDARY_" + parent.substring(parent.length() - 1), 1.);
                if (parent.length() >= 2)
                    Tools.addFeature(features, inVocab + "SUFFIX_" + affix + "_BOUNDARY_" + parent.substring(parent.length() - 2), 1.);
            }

            //DELETE specific features
            int parentLen = parent.length();
            Tools.addFeature(features, inVocab+"DELETE_"+parent.charAt(parentLen - 1), 1.);
        }

        else if (type == PREFIX) {
            assert word.length() != parent.length();
            affix = word.substring(0, word.length() - parent.length());
            if(affix.length() > MAX_AFFIX_LENGTH || !prefixes.contains(affix)) affix = "UNK";
            if(!affix.equals("UNK")) {
                Tools.addFeature(features, inVocab + "PREFIX_" + affix, 1);
            }

            if(AFFIX_NEIGHBORS && prefixNeighbor.containsKey(affix)) {
                int i = 0;
                for(String neighbor : prefixNeighbor.get(affix).keySet()) {
                    if (word2Cnt.containsKey(neighbor + parent)) {
                        Tools.addFeature(features, "COR_P_" + affix, 1.);
                        break;
                    }
                    i++;
                    if(i==TOP_AFFIX_NEIGHBORS) break;
                }
            }

            //context features
            if(AFFIX_CONTEXT) {
                Tools.addFeature(features, inVocab + "PREFIX_" + affix + "_BOUNDARY_" + parent.substring(0, 1), 1.);
                if (parent.length() >= 2)
                    Tools.addFeature(features, inVocab + "PREFIX_" + affix + "_BOUNDARY_" + parent.substring(0, 2), 1.);
            }
        }

        //BIAS feature
        Tools.addFeature(features, "BIAS", 1.);

        //cache features
        cacheFeatures(word, parent, type, features);


        return features;
    }

    static HashMap<Integer, Double> getStopFeatures(String word) {
        HashMap<Integer, Double> features = new HashMap<Integer, Double>();

        if(word.length()>=2) {  //check is only to avoid null exception
            Tools.addFeature(features, "STP_E_" + word.substring(word.length() - 2), 1.);
            Tools.addFeature(features, "STP_B_" + word.substring(0, 2), 1.);
        }


        //max dot feature
        if(DOT) {
            double maxDot = word2MaxDot.get(word);
            Tools.addFeature(features, "STP_COS_" + (int) (10 * maxDot), 1.);
        }

        //length feature
        Tools.addFeature(features, "STP_LEN_"+word.length(), 1.);

        //BIAS feature
        Tools.addFeature(features, "BIAS", 1.);

        return features;
    }


    // -------------------------------------- Affix Pre-computations ------------------------------------------------


    //get most frequent affixes
    void selectMostFrequentAffixes() throws IOException {
        HashMap<String, Integer> suffixCnt = new HashMap<String, Integer>();
        HashMap<String, Integer> prefixCnt = new HashMap<String, Integer>();
        for (String word : word2Cnt.keySet()) {
            if(word2Cnt.get(word) < FREQ_THRESHOLD) continue;
            for (int i = 1; i < word.length(); i++) {
                String left = word.substring(0, i);
                String right = word.substring(i);

                //suffix case
                Integer cnt = word2Cnt.get(left);
                if (cnt != null && cnt > AFFIX_FREQ_THRESHOLD && right.length() <= MAX_AFFIX_LENGTH)
                    if (suffixCnt.containsKey(right))
                        suffixCnt.put(right, suffixCnt.get(right) + 1);
                    else
                        suffixCnt.put(right, 1);

                //prefix case
                cnt = word2Cnt.get(right);
                if (cnt != null && cnt > AFFIX_FREQ_THRESHOLD && left.length() <= MAX_AFFIX_LENGTH)
                    if (prefixCnt.containsKey(left))
                        prefixCnt.put(left, prefixCnt.get(left) + 1);
                    else
                        prefixCnt.put(left, 1);
            }
        }

        //sort and take top
        Map<String, Integer> sortedSuffixes = Tools.sortByValue(suffixCnt);
        Map<String, Integer> sortedPrefixes = Tools.sortByValue(prefixCnt);
        int i = 0;
        for (String suffix : sortedSuffixes.keySet()) {
            suffixes.add(suffix);
            i++;
            if (i == TOP_AFFIX_SELECT) break;
        }
        i = 0;
        for (String prefix : sortedPrefixes.keySet()) {
            prefixes.add(prefix);
            i++;
            if (i == TOP_AFFIX_SELECT) break;
        }

        if(DEBUG) {
            System.out.println(suffixes);
            System.out.println(prefixes);
        }

        //NEW: add suffixNeighbors
        if(AFFIX_NEIGHBORS) {
            suffixNeighbor = Tools.computeAffixCorrelation(suffixes, 's');
            prefixNeighbor = Tools.computeAffixCorrelation(prefixes, 'p');
        }
    }

    //Heuristic function to prune out
    static boolean checkHeuristic(String word, String parent, int type) {
        double dot_threshold = 0.0;
        if(type == MODIFY)
            dot_threshold = 0.5;
        Integer wordCnt = word2Cnt.get(parent);
        if(wordCnt!= null && wordCnt > HEURISTIC_FREQ_THRESHOLD)
            if(2*parent.length() >= word.length())
                if(Tools.dot(word, parent) > dot_threshold) //the dot takes care of the non-exist case by returning 0.
                    return true;
        return false;
    }

    // -------------------------------------- Candidate Generation ------------------------------------------------

    //generate all possible parent candidates - if heuristic is true, will use the heuristic to prune
    static ArrayList<Pair<String, Integer>> getCandidates(String word, boolean heuristic) {
        ArrayList<Pair<String, Integer>> candidates = new ArrayList<Pair<String, Integer>>();

        for(int i=1;i<word.length(); i++) {
            //suffix case
            String parent = word.substring(0, i);
            if(2 * parent.length() >= word.length()) { //be careful here
                if (!heuristic || checkHeuristic(word, parent, SUFFIX))
                    candidates.add(new MutablePair<String, Integer>(parent, SUFFIX));
            }

            //REPEAT and MODIFY case
            if(2 * parent.length() >= word.length()) { //be careful here  //TODO
                if (REPEAT_ON)
                    if (word.charAt(i - 1) == word.charAt(i)) {
                        if (!heuristic || checkHeuristic(word, parent, REPEAT))
                            candidates.add(new MutablePair<String, Integer>(parent, REPEAT));
                    }
                int n = parent.length();
                if (MODIFY_ON && ALPHABET.contains(parent.substring(n-1))) {

                    for(int q=0;q<ALPHABET.length();q++) {
                        if(ALPHABET.charAt(q) == parent.charAt(n-1)) continue;
                        String newParent = parent.substring(0, n-1)+ALPHABET.charAt(q);

                        if(word2Cnt.containsKey(newParent) && word2Cnt.get(newParent) > HEURISTIC_FREQ_THRESHOLD && Tools.dot(word, newParent) > 0.2)
                            candidates.add(new MutablePair<String, Integer>(newParent, MODIFY));

                    }
                }
                if (DELETE_ON && i < word.length()-1 && suffixes.contains(word.substring(i))) {

                    for(int q=0;q<ALPHABET.length();q++) {
                        String newParent = parent+ALPHABET.charAt(q);
                        if(word.contains(newParent)) continue;
                        if(word2Cnt.containsKey(newParent) && word2Cnt.get(newParent) > HEURISTIC_FREQ_THRESHOLD && Tools.dot(word, newParent) > 0)
                            candidates.add(new MutablePair<String, Integer>(newParent, DELETE));
                    }
                }
            }

            //prefix case
            parent = word.substring(i);
            if(2 * parent.length() >= word.length())
                if (!heuristic || checkHeuristic(word, parent, PREFIX))
                    candidates.add(new MutablePair<String, Integer>(parent, PREFIX));
        }

        //stop case
        if(!heuristic || candidates.size()==0)
            candidates.add(new MutablePair<String, Integer>(word, STOP));
        return candidates;
    }



    //wrapper
    static ArrayList<Pair<String, Integer>> getCandidates(String word) {
        return getCandidates(word, false);  //no heuristics used
    }


    // ------------------------------------- Inference -------------------------------------------------

    //predict the parent of a word
    static Pair<String, Integer> predict(String word) {
        ArrayList<Pair<String, Integer>> candidateParents = getCandidates(word); //no need to restrict to heuristics
        double bestScore = -Double.MAX_VALUE, score;
        Pair<String, Integer> bestParentAndType = null;

        //for multinomial
        ArrayList<Sample.MultinomialObject> multinomial = new ArrayList<Sample.MultinomialObject>();
        double Z = 0.;

        for(Pair<String, Integer> parentAndType : candidateParents ) {
            String parent = parentAndType.getKey();
            int type = parentAndType.getValue();
            score = scoreParent(word, parent, type);
            if(type == STOP) score *= STOP_FACTOR;
            if(score > bestScore) {
                bestScore = score;
                bestParentAndType =  parentAndType;
            }
            multinomial.add(new Sample.MultinomialObject(parent, type, Math.exp(score)));
            Z += Math.exp(score);
        }

        //normalize the multinomial
        for(Sample.MultinomialObject obj: multinomial)
            obj.score /= Z;


        Map<String, Double> f2W = new HashMap<String, Double>();
        for(int featureIndex : getFeatures(word, bestParentAndType.getKey(),bestParentAndType.getValue()).keySet())
            f2W.put(index2Feature.get(featureIndex), feature2Weight.get(index2Feature.get(featureIndex)));


        return bestParentAndType;
    }

    //predict the top k parents of a word
    static ArrayList<Pair<String, Integer>> predict(String word, int topK) {
        ArrayList<Pair<String, Integer>> candidateParents = getCandidates(word); //no need to restrict to heuristics
        double score;
        HashMap<Pair<String,Integer>, Double> parentDic = new HashMap<Pair<String, Integer>, Double>();


        double Z = 0.;

        for(Pair<String, Integer> parentAndType : candidateParents ) {
            String parent = parentAndType.getKey();
            int type = parentAndType.getValue();
            score = scoreParent(word, parent, type);
            parentDic.put(parentAndType, score);
        }

        //sort
        ArrayList<Pair<String, Integer>> bestParents = new ArrayList<Pair<String, Integer>>();
        for(Pair<String, Integer> pair : Tools.sortByValue(parentDic).keySet()) {
            bestParents.add(pair);
            topK--;
            if(topK <=0 ) break;
        }

        return bestParents;

    }

    //function to choose predictions from the sampled points
    static ArrayList<Integer> predictSampledPoints(ArrayList<Integer> sampledPoints) {
        ArrayList<Integer> points =  new ArrayList<Integer>();
        int lastPoint = -1;
        for(int point : sampledPoints) {
            if (point == -1) break;
            if (point < lastPoint) break;
            points.add(point);
            lastPoint = point;
        }
        return points;
    }

    static String segment(String word) {
        //produces a segmentation
        if(word.length() < MIN_SEG_LENGTH) return word;

        if(word.contains("-")) {
            String [] parts = word.split("-");
            String seg = segment(parts[0]);
            for(int i=1;i<parts.length; i++)
                seg += ("-"+segment(parts[i]));
            return seg;
        }

        if(word.contains("'")) {
            String [] parts = word.split("'");
//            assert parts.length<=2; //just a check
            String suffix = "'";
//            if (parts.length==2)
            suffix += parts[parts.length-1];
            if(suffixes.contains(suffix))
                Tools.incrementMap(suffixDist, suffix);
            String seg = segment(parts[0]);
            for(int i=1;i<parts.length; i++)
                seg += ("-'"+segment(parts[i]));
            return seg;
        }

        Pair<String, Integer> parentAndType = predict(word);
        if(parentAndType == null) return word;
        String parent = parentAndType.getKey();
        int type = parentAndType.getValue();

        if(type == STOP)
            return word;
        int parentLen = parent.length();

        //IMP:  cases for REPEAT, MODIFY, etc here
        //else segment
        //suffix case
        if(type == SUFFIX) {
            String suffix = word.substring(parentLen);
            if(suffixes.contains(suffix))
                Tools.incrementMap(suffixDist, suffix);
            return segment(parent) + "-" + suffix;
        }
        else if(type == REPEAT)
            return segment(parent)+word.charAt(parentLen)+"-"+word.substring(parentLen + 1);
        else if(type==MODIFY) {
            //TODO : check
            String parentSeg = segment(parent);
            return parentSeg.substring(0, parentSeg.length()-1) + word.charAt(parentLen-1) + "-" + word.substring(parentLen);
        }
        else if(type==DELETE) {
            String parentSeg = segment(parent);
            int parentSegLen = parentSeg.length();
            if(parentSeg.charAt(parentSegLen-2) == '-')
                return parentSeg.substring(0, parentSegLen-1) + word.substring(parentLen-1);
            else
                return parentSeg.substring(0, parentSegLen-1) + "-" + word.substring(parentLen-1);
        }
        //prefix case
        else if(type == PREFIX) {
            String prefix = word.substring(0, word.length() - parentLen);
            if(prefixes.contains(prefix))
                Tools.incrementMap(prefixDist, prefix);
            return prefix + "-" + segment(parent);
        }
        else if(type == COMPOUND)
            return segment(word.substring(0, parentLen))+"-"+segment(word.substring(parentLen));

        //null should not be returned at all. Having this to debug, instead of an assert
        return null;
    }

    //returns the logScore
    static double scoreParent(String word, String parent, int type) {
        return Tools.featureWeightProduct(getFeatures(word, parent, type));
    }


    // -------------------------------------- Feature Caching ------------------------------------------------


    //Functions for Caching features
    static void cacheFeatures(String word, String parent, int type, HashMap<Integer, Double> features) {
        if(!w2P2TypeFeatures.containsKey(word))
            w2P2TypeFeatures.put(word, new HashMap<String, HashMap<Integer, HashMap<Integer, Double>>>());
        if(!w2P2TypeFeatures.get(word).containsKey(parent))
            w2P2TypeFeatures.get(word).put(parent, new HashMap<Integer, HashMap<Integer, Double>>());
        if(!w2P2TypeFeatures.get(word).get(parent).containsKey(type))
            w2P2TypeFeatures.get(word).get(parent).put(type, features);

        //else just ignore
    }

    static boolean checkCacheExists(String word, String parent, int type) {
        if(w2P2TypeFeatures.containsKey(word))
            if(w2P2TypeFeatures.get(word).containsKey(parent))
                if(w2P2TypeFeatures.get(word).get(parent).containsKey(type))
                    return true;

        return false;
    }


    // -------------------------------------- For LBFGS (OLD) ------------------------------------------------
    //return the heuristic sum part in the objective
    static double logSumPartObjective(String word, boolean heuristic) {
        ArrayList<Double> A = new ArrayList<Double>();
        for(Pair<String, Integer> parentAndType : getCandidates(word))
            A.add(Tools.featureWeightProduct(getFeatures(word, parentAndType.getKey(), parentAndType.getValue())));

        return Tools.logSumOfExponentials(A);
    }

    //return the heuristic sum part in the objective
    static HashMap<Integer, Double> gradObjective(String word, boolean heuristic) {
        HashMap<Integer, Double> grad = new HashMap<Integer, Double>();
        HashMap<Integer, Double> tmpMap;
        double totalVal = 0.;
        for(Pair<String, Integer> parentAndType  : getCandidates(word)) {
            tmpMap = getFeatures(word, parentAndType.getKey(), parentAndType.getValue());
            double expVal = Math.exp(Tools.featureWeightProduct(tmpMap));
            totalVal += expVal;
            Tools.updateMap(grad, tmpMap, expVal);
        }

        for(Map.Entry<Integer, Double> entry : grad.entrySet())
            grad.put(entry.getKey(), entry.getValue()/totalVal);

        return grad;
    }


}
