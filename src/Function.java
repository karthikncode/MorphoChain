import lbfgsb.DifferentiableFunction;
import lbfgsb.FunctionValues;

import java.util.HashMap;

/**
 * Created by ghostof2007 on 5/6/14.
 *
 * Function to be used for LBFGS
 */
public class Function implements DifferentiableFunction {

    // -------------------------------------- LBFGS-B ---------------------------------------------------------


    @Override
    public FunctionValues getValues(double[] point) {

        assert (point.length == MorphoChain.weights.size());
        //copy the weights
        for(int i=0;i<point.length;i++)
            MorphoChain.weights.set(i, point[i]);

        //clear all static variables
        if(MorphoChain.EM_ON) {
            EM.functionValue = 0;
            EM.featureGradients.clear();

            int i = 0;
            //calculate values for EM_ON
            for(String word : MorphoChain.word2Cnt.keySet()) {
                if(MorphoChain.word2Cnt.get(word) < MorphoChain.FREQ_THRESHOLD) continue;
                if(Main.SOFT_EM)
                    EM.calculateProbsSoft(word);
                else
                    EM.calculateProbs(word);
                System.err.print("\r" + (i++) + "/" + MorphoChain.word2Cnt.size());
            }
            System.err.println();
        }

        //Store the final weights (for debugging)
        for(String feature : MorphoChain.feature2Index.keySet())
            MorphoChain.feature2Weight.put(feature, point[MorphoChain.feature2Index.get(feature)]);
        //sort the weights
        MorphoChain.feature2Weight = Tools.sortByValue(MorphoChain.feature2Weight);

        System.err.println("**********");

        return new FunctionValues(functionValue(point), gradient(point));
    }

    double functionValue(double[] iterWeights) {

        if(MorphoChain.EM_ON) {
            double sum = EM.functionValue;
            //regularization
            for(double weight : iterWeights) {
                sum -= MorphoChain.LAMBDA_EM * Math.pow(weight, 2);
            }
            sum *= -1;
            System.err.println("F = "+sum);
            assert(!Double.isNaN(sum));
            return sum; //return negative since minimizing
        }


        double sum = 0.;
        //TODO : parallelizable
        for(String word : MorphoChain.word2Cnt.keySet()) {
            if(MorphoChain.word2Cnt.get(word) < MorphoChain.FREQ_THRESHOLD) continue;
            sum += (MorphoChain.logSumPartObjective(word, true) - MorphoChain.logSumPartObjective(word, false));
        }

        //regularization
        for(double weight : iterWeights) {
            sum -= MorphoChain.LAMBDA * Math.pow(weight, 2);
        }

        sum *= -1;              //take negative since minimizing
        return sum;
    }

    double[] gradient(double[] iterWeights) {
        if(MorphoChain.EM_ON) {
//            assert (EM.featureGradients.size() == iterWeights.length);
            //just copy values
            double [] grad = new double[iterWeights.length];
            for(int i=0;i<grad.length; i++)
//            for(int i : EM.featureGradients.keySet())
                grad[i] = -EM.featureGradients.get(i); //negative since minimizing

            //regularization - adding positive values since the sign has been changed already
            for(int i=0;i<grad.length; i++)
                grad[i] += 2 * MorphoChain.LAMBDA_EM * iterWeights[i];

            System.err.println("|g| = "+Tools.squareSum(grad));
            assert(!Double.isNaN(Tools.squareSum(grad)));
            return grad;
        }

        HashMap<Integer, Double> featureGradient = new HashMap<Integer, Double>();
        //TODO : parallelizable
        for(String word : MorphoChain.word2Cnt.keySet()) {
            if(MorphoChain.word2Cnt.get(word) < MorphoChain.FREQ_THRESHOLD) continue;
            HashMap<Integer, Double> tmpMapC = MorphoChain.gradObjective(word, true);
            HashMap<Integer, Double> tmpMapD = MorphoChain.gradObjective(word, false);

            Tools.updateMap(featureGradient, tmpMapC);      // to take care of heuristic - rest case
            Tools.updateMap(featureGradient, tmpMapD, -1.); //TODO : check if necessary
        }

        double [] grad = new double[iterWeights.length];
        featureGradient.remove(-1);
        for(int i : featureGradient.keySet())
            grad[i] = -(featureGradient.get(i)); //take negative since minimizing; also normalizing

        //regularization - adding positive values since the sign has been changed already
        for(int i=0;i<grad.length; i++)
            grad[i] += 2 * MorphoChain.LAMBDA * iterWeights[i];
        return grad;
    }


}

