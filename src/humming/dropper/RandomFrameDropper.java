package humming.dropper;

import echowand.net.Frame;
import echowand.net.SubnetException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class RandomFrameDropper implements FrameDropper {
    private static final Logger LOGGER = Logger.getLogger(RandomFrameDropper.class.getName());
    private static final String CLASS_NAME = RandomFrameDropper.class.getName();
    
    private double sendDropRate;
    private double receiveDropRate;
    private Random rand;
    private int accuracy = 100000;
    
    public RandomFrameDropper(double dropRate) {
        this.sendDropRate = dropRate;
        this.receiveDropRate = dropRate;
        
        rand = new Random();
    }
    
    public RandomFrameDropper(double sendDropRate, double receiveDropRate) {
        this.sendDropRate = sendDropRate;
        this.receiveDropRate = receiveDropRate;
        
        rand = new Random();
    }

    @Override
    public boolean shouldDropSend(Frame frame) throws SubnetException {
        if ((rand.nextInt(accuracy) / (double)accuracy) < sendDropRate) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "shouldSend", "drop frame: " + frame);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean shouldDropReceive(Frame frame) throws SubnetException {
        for (;;) {
            if ((rand.nextInt(accuracy) / (double)accuracy) < receiveDropRate) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "shouldReceive", "drop frame: " + frame);
                return true;
            } else {
                return false;
            }
        }
    }
    
}
