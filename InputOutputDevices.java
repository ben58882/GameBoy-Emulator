import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JFrame;
public class InputOutputDevices implements Memory, KeyListener{
  
    private int[] directionButtons = new int[] {1, 1, 1, 1}; //Down, Up, Left, Right
    private int[] actionButtons = new int[] {1, 1, 1, 1}; //: Start, Select, B, A
    private int joyPadSelectors = 3;
    
    @Override
    public int read(int address){
        if(address == 0xFF00){
            if(joyPadSelectors == 0){
                return (extractKeys(2) | (joyPadSelectors << 4) | 0xC0);
            }
            else if(joyPadSelectors == 1){
                return (extractKeys(1) | (joyPadSelectors << 4) | 0xC0);
            }
            else if(joyPadSelectors == 2){
                return (extractKeys(0) | (joyPadSelectors << 4) | 0xC0);
            }
            else{
                return 0xFF;
            }
        }
        else{
            //HAVE YET to implement
            return 0;
        }
    }

    private int extractKeys(int id){
        int val = 0;
        if(id == 0){
            for(int i = 0; i < 4; i++){
                val |= (directionButtons[i] << i);
            }
            return val;
        }
        else if(id == 1){
            for(int i = 0; i < 4; i++){
                val |= (actionButtons[i] << i);
            }
            return val;
        }
        else if(id == 2){
            for(int i = 0; i < 4; i++){
                val |= ((directionButtons[i] << i) & (actionButtons[i] << i));
            }
            return val;
        }
        else{
            return 0xF;
        }
    }

    @Override 
    public void write(int address, int value){
        if(address == 0xFF00){
            this.joyPadSelectors = (0x30 & value) >> 4;
        }
        else{

        }
    }

    public InputOutputDevices(JFrame window) {
        window.addKeyListener(this);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        setKey(e.getKeyCode(), true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        setKey(e.getKeyCode(), false);
    }

    private void setKey(int keyCode, boolean isPressed) {
        int state = isPressed ? 0 : 1;
        switch (keyCode) {
            // Direction Array: 0=Right, 1=Left, 2=Up, 3=Down
            case KeyEvent.VK_RIGHT: this.directionButtons[0] = state; break;
            case KeyEvent.VK_LEFT:  this.directionButtons[1] = state; break;
            case KeyEvent.VK_UP:    this.directionButtons[2] = state; break;
            case KeyEvent.VK_DOWN:  this.directionButtons[3] = state; break;
            
            // Action Array: 0=A, 1=B, 2=Select, 3=Start
            case KeyEvent.VK_A:     this.actionButtons[0] = state; break; // A button
            case KeyEvent.VK_S:     this.actionButtons[1] = state; break; // B button
            case KeyEvent.VK_SHIFT: this.actionButtons[2] = state; break; // Select
            case KeyEvent.VK_ENTER: this.actionButtons[3] = state; break; // Start
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {} // Unused
}