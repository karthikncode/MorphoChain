import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Created by ghostof2007 on 5/8/14.
 * Different evaluation routines
 */
public class Evaluate {

    static HashMap<String, String> predictedSegmentations = new HashMap<String, String>();
    static HashMap<String, String> incorrectSegmentations = new HashMap<String, String>();
    static HashMap<String, String> correctSegmentations = new HashMap<String, String>();

    static HashSet<Integer> getSegPoints(String segmentation) {
        HashSet<Integer> segPoints = new HashSet<Integer>();
        int i=0;
        for(char ch : segmentation.toCharArray()) {
            if(ch == '-')
                segPoints.add(i);
            else
                i++;
        }
        return segPoints;
    }

    static double [] evaluateSegmentationPoints(String predSeg, ArrayList<String> goldSegs) {
        //find the best match over different points
        double bestCorrect = 0., bestTotal =0., minBestTotal = 100.;
        HashSet<Integer> predPoints = getSegPoints(predSeg);
        int predSize = predPoints.size();
        for(String goldSeg : goldSegs) {
            HashSet<Integer> goldPoints = getSegPoints(goldSeg);
            int goldSize = goldPoints.size();
            goldPoints.retainAll(predPoints); //IMP  : goldPoints is modified here to get the intersection of points
            int correct = goldPoints.size();
            if(correct > bestCorrect || (correct == bestCorrect && goldSize < bestTotal)) {
                bestCorrect = correct;
                bestTotal = goldSize;
            }
            if(goldSize < minBestTotal)
                minBestTotal = goldSize;
        }
        if(bestTotal == 0)
            bestTotal = minBestTotal;

        return new double[]{bestCorrect, bestTotal, predSize};
    }


    static double evaluateSegmentation() {
        //uses static variables from the Model class directly
        MorphoChain.TEST = true;
        double correct = 0., predTotal =0., goldTotal =0.;

        System.out.println("Evaluating segmentations...");
        predictedSegmentations.clear();
        incorrectSegmentations.clear();
        correctSegmentations.clear();

        for(Pair<String, ArrayList<String>> entry : MorphoChain.goldSegs) {
            //segment without explicit chain
            String predSeg = MorphoChain.segment(entry.getKey());

            double [] retValues = evaluateSegmentationPoints(predSeg, entry.getValue());

            correct += retValues[0];goldTotal += retValues[1];predTotal += retValues[2];
            predictedSegmentations.put(entry.getKey(), predSeg);
            if(retValues[1] != retValues[2])
                incorrectSegmentations.put(entry.getKey(), predSeg+" : "+entry.getValue());
            else
                correctSegmentations.put(entry.getKey(), predSeg+" : "+entry.getValue());

        }
        System.out.println("Done.");
        System.out.println("Incorrect Segmentations");
        printSegmentations(incorrectSegmentations);
        System.out.println("Correct Segmentations");
        printSegmentations(correctSegmentations);

        double precision = correct/predTotal, recall = correct/goldTotal;
        double f1 = (2*precision*recall)/(precision+recall);
        System.out.println("Correct: "+correct+" GoldTotal: " + goldTotal+ " PredTotal: "+predTotal);
        System.out.println("Precision: " + precision + " Recall: " + recall + " F1: " + f1);
        MorphoChain.TEST = false;
        return f1;
    }


    private static void printSegmentations(HashMap<String, String> segmentations) {
        //print the segmentations
        for(Map.Entry<String, String> entry : segmentations.entrySet())
            System.out.println(entry.getKey()+" # "+entry.getValue());
    }
}
