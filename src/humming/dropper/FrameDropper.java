package humming.dropper;

import echowand.net.Frame;
import echowand.net.SubnetException;

/**
 *
 * @author ymakino
 */
public interface FrameDropper {
    boolean shouldDropSend(Frame frame) throws SubnetException ;
    boolean shouldDropReceive(Frame frame) throws SubnetException ;
}
