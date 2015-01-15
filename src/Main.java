import lbfgsb.LBFGSBException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;

/**
 * Created by ghostof2007 on 5/6/14.
 * Main Class
 */
public class Main {

    static boolean SOFT_EM = true;
    static boolean PRODUCE_SEGMENTATIONS = false;

    static Random rn = new Random();

    public static void main(String [] args) throws IOException, InterruptedException, LBFGSBException {

        rn.setSeed(10);
        Properties prop = new Properties();
        InputStream input = null;
        String paramsFile;
        if(args.length>0) {
            paramsFile = args[0];
        }
        else {
            paramsFile = "params.properties";
        }

        //get params
        try {

            input = new FileInputStream(paramsFile);

            // load a properties file
            prop.load(input);

            // get the property values
            MorphoChain.FREQ_THRESHOLD = Integer.parseInt(prop.getProperty("FREQ_THRESHOLD"));
            if(prop.getProperty("VECTOR_SIZE") != null)
                MorphoChain.VECTOR_SIZE = Integer.parseInt(prop.getProperty("VECTOR_SIZE"));
            MorphoChain.LAMBDA_EM=Double.parseDouble(prop.getProperty("LAMBDA_EM"));
            MorphoChain.HEURISTIC_FREQ_THRESHOLD=Integer.parseInt(prop.getProperty("HEURISTIC_FREQ_THRESHOLD"));
            MorphoChain.AFFIX_FREQ_THRESHOLD=Integer.parseInt(prop.getProperty("AFFIX_FREQ_THRESHOLD"));
            MorphoChain.MIN_SEG_LENGTH=Integer.parseInt(prop.getProperty("MIN_SEG_LENGTH"));
            MorphoChain.TOP_AFFIX_SELECT=Integer.parseInt(prop.getProperty("TOP_AFFIX_SELECT"));
            MorphoChain.wordVectorFile=prop.getProperty("wordVectorFile");
            MorphoChain.wordListFile=prop.getProperty("wordListFile");
            if(PRODUCE_SEGMENTATIONS) MorphoChain.testListFile=prop.getProperty("testListFile");
            MorphoChain.goldSegFile=prop.getProperty("goldSegFile");
            if(prop.getProperty("alphabet") != null)
                MorphoChain.ALPHABET = prop.getProperty("alphabet");


        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        MorphoChain model = new MorphoChain();
        model.initialize();

        //run EM
        MorphoChain.EM_ON = true;
        EM.MStep();


        // imp : Routines for evaluating segmentations (originally used in paper)
        Evaluate.evaluateSegmentation();

        System.out.println("Done");
    }
}
