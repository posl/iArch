<#-- FreeMarker template to random selection at generated Aspect. -->
<#assign aDateTime = .now>
/*
Generated by iArch
Time: ${aDateTime?iso_local}
*/

package ${PACKAGE_NAME};

import ${PACKAGE_NAME}.SuppressArchfaceWarnings;

import java.util.Random;

@${SUPPRESS_ANNOTATION_NAME}({"all"})
public abstract class ${ABSTRACT_CLASS_NAME} {

    /* CONFIG BEGIN */
    
    // seed to random execution, set this to 0l to use default seed
    private static long seed = 0l;
    
    // to initialize random at every execution or not
    private static boolean initSeed = false;
    
    /* CONFIG END */

    public static long getSeed() {return seed;}
    public static void setSeed(long s) {seed = s;}

    public static boolean getInitSeed() {return initSeed;}
    public static void setInitSeed(boolean i) {initSeed = i;}

    private static Random random;

    static {
        random = seed == 0l ? new Random() : new Random(seed);
    }

    protected static int weightSwitch(double... weights) {
        if (initSeed) {
            random = seed == 0l ? new Random() : new Random(seed);
        }
        double sum = 0d;
        for (double weight : weights) {
            sum += weight;
        }
        double randomDouble = random.nextDouble() * sum;

        double runningSum = 0d;
        int index = 0;
        for (double weight : weights) {
            runningSum += weight;
            if (runningSum > randomDouble) {
                return index;
            }
            index++;
        }
        return 0;
    }

}