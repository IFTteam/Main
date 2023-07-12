package springredis.demo.entity;

import springredis.demo.structures.FuzzyStringMatch.MatchingBlock;
import springredis.demo.Service.impl.FuzzyStringMatchImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 这部分代码用于fuzzystring match
 * 来源于https://github.com/xdrop/fuzzywuzzy
 * 将java fuzzywuzzy数据包转为java，我将源代码迁移过来，有微调
 */

/**
 * Partial ratio of similarity
 */
public class PartialRatio{

    /**
     * Computes a partial ratio between the strings
     *
     * @param s1 Input string
     * @param s2 Input string
     * @return The partial ratio
     */

    public int apply(String s1, String s2) {

        String shorter;
        String longer;

        if (s1.length() <= s2.length()){

            shorter = s1;
            longer = s2;

        } else {

            shorter = s2;
            longer = s1;

        }

        MatchingBlock[] matchingBlocks = FuzzyStringMatchImpl.getMatchingBlocks(shorter, longer);

        List<Double> scores = new ArrayList<>();

        for (MatchingBlock mb : matchingBlocks) {

            int dist = mb.dpos - mb.spos;

            int long_start = dist > 0 ? dist : 0;
            int long_end = long_start + shorter.length();

            if(long_end > longer.length()) long_end = longer.length();

            String long_substr = longer.substring(long_start, long_end);

            double ratio = FuzzyStringMatchImpl.getRatio(shorter, long_substr);

            if (ratio > .995) {
                return 100;
            } else {
                scores.add(ratio);
            }

        }

        return (int) Math.round(100 * Collections.max(scores));

    }


}