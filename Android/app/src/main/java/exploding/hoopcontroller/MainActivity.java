package exploding.hoopcontroller;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.larswerkman.holocolorpicker.ColorPicker;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    BluetoothController bluetoothController;

    // Controls
    ColorPicker colorPicker;
    Button setColorButton;
    ToggleButton toggleChaseBtn;
    ToggleButton toggleRainbowBtn;
    ToggleButton toggleRainbowCycleBtn;
    ToggleButton toggleSparkleBtn;
    ToggleButton toggleStrobeBtn;
    ToggleButton toggleSolidBtn;
    ToggleButton toggleOnOffBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize bluetooth controller
        // Check for Bluetooth support and then check to make sure it is turned on
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter != null) {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }

        bluetoothController = new BluetoothController(btAdapter);

        // Initialize UI controls
        colorPicker = (ColorPicker) findViewById(R.id.picker);
        setColorButton = (Button) findViewById(R.id.setColorButton);
        toggleChaseBtn = (ToggleButton) findViewById(R.id.toggleChase);
        toggleRainbowBtn = (ToggleButton) findViewById(R.id.toggleRainbow);
        toggleRainbowCycleBtn = (ToggleButton) findViewById(R.id.toggleRainbowCycle);
        toggleSparkleBtn = (ToggleButton) findViewById(R.id.toggleSparkle);
        toggleStrobeBtn = (ToggleButton) findViewById(R.id.toggleStrobe);
        toggleSolidBtn = (ToggleButton) findViewById(R.id.toggleSolid);
        toggleOnOffBtn = (ToggleButton) findViewById(R.id.toggleOnOff);

        // Set event handlers
        toggleChaseBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onChaseChanged(buttonView, isChecked);
            }
        });

        toggleRainbowBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onRainbowChanged(buttonView, isChecked);
            }
        });

        toggleRainbowCycleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onRainbowCycleChanged(buttonView, isChecked);
            }
        });

        toggleSparkleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onSparkleChanged(buttonView, isChecked);
            }
        });

        toggleStrobeBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onStrobeChanged(buttonView, isChecked);
            }
        });

        toggleSolidBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onSolidChanged(buttonView, isChecked);
            }
        });

        toggleOnOffBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onOnOffChanged(buttonView, isChecked);
            }
        });
    }

    protected void setColorClick(View view) {
        int c = colorPicker.getColor();
        colorPicker.setOldCenterColor(c);

        bluetoothController.setColor(c);
    }

    protected void onChaseChanged(CompoundButton buttonView, boolean isChecked) {
        disableAllButtons();
        if(isChecked) {
            buttonView.setChecked(true);
        }

        bluetoothController.setEffect(0);
    }

    protected void onRainbowChanged(CompoundButton buttonView, boolean isChecked) {
        disableAllButtons();
        if(isChecked) {
            buttonView.setChecked(true);
        }

        bluetoothController.setEffect(1);
    }

    protected void onRainbowCycleChanged(CompoundButton buttonView, boolean isChecked) {
        disableAllButtons();
        if(isChecked) {
            buttonView.setChecked(true);
        }

        bluetoothController.setEffect(2);
    }

    protected void onSparkleChanged(CompoundButton buttonView, boolean isChecked) {
        disableAllButtons();
        if(isChecked) {
            buttonView.setChecked(true);
        }

        bluetoothController.setEffect(3);
    }

    protected void onStrobeChanged(CompoundButton buttonView, boolean isChecked) {
        disableAllButtons();
        if(isChecked) {
            buttonView.setChecked(true);
        }

        bluetoothController.setEffect(4);
    }

    protected void onSolidChanged(CompoundButton buttonView, boolean isChecked) {
        disableAllButtons();
        if(isChecked) {
            buttonView.setChecked(true);
        }

        bluetoothController.setEffect(5);
    }

    protected void onOnOffChanged(CompoundButton buttonView, boolean isChecked) {
        disableAllButtons();
        if(isChecked) {
            buttonView.setChecked(true);
        }

        bluetoothController.setEffect(6);
    }

    @Override
    public void onResume() {
        super.onResume();

        bluetoothController.resume();
    }

    @Override
    public void onPause() {
        super.onPause();

        bluetoothController.pause();
    }

    private void disableAllButtons() {
        toggleChaseBtn.setChecked(false);
        toggleRainbowBtn.setChecked(false);
        toggleRainbowCycleBtn.setChecked(false);
        toggleSparkleBtn.setChecked(false);
        toggleStrobeBtn.setChecked(false);
        toggleSolidBtn.setChecked(false);
        toggleOnOffBtn.setChecked(false);
    }
}
