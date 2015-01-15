
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Created by ghostof2007 on 5/21/14.
 * Functions to sample chains, etc
 */
public class Sample {

    static class MultinomialObject {
        String parent;
        int type;
        double score;

        MultinomialObject(String parent_, int type_, double score_) {
            parent = parent_;
            type = type_;
            score = score_;
        }
    }

}
