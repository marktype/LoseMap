package net.wyxj.vehicle;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.Menu;
import android.widget.EditText;

public class VehicleActivity extends Activity {
    Bundle bundle;
    // 汽车参数
    public double mass;
    public double sprung;
    public double track;
    public double roll;
    public double height;
    public double suspension;
    public double nonsense1;
    public double nonsense2;

    private EditText editMass;
    private EditText editSprung;
    private EditText editTrack;
    private EditText editRoll;
    private EditText editHeight;
    private EditText editSuspension;
    private EditText editNonsense1;
    private EditText editNonsense2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle);

        bundle = getIntent().getExtras();
        mass = bundle.getDouble("mass", 18300);
        sprung = bundle.getDouble("sprung", 14414);
        track = bundle.getDouble("track", 2.1);
        roll = bundle.getDouble("roll", 0.8);
        height = bundle.getDouble("height", 1);
        suspension = bundle.getDouble("suspension", 300000);
        nonsense1 = bundle.getDouble("nonsense1", 5);
        nonsense2 = bundle.getDouble("nonsense2", 487050);

        editMass = (EditText)findViewById(R.id.vehicle_input_mass);
        editSprung = (EditText)findViewById(R.id.vehicle_input_sprung);
        editTrack = (EditText)findViewById(R.id.vehicle_input_track);
        editRoll = (EditText)findViewById(R.id.vehicle_input_roll);
        editHeight = (EditText)findViewById(R.id.vehicle_input_height);
        editSuspension = (EditText)findViewById(R.id.vehicle_input_suspension);
        editNonsense1 = (EditText)findViewById(R.id.vehicle_input_view3);
        editNonsense2 = (EditText)findViewById(R.id.vehicle_input_view4);

        editMass.setText(""+mass);
        editSprung.setText(""+sprung);
        editTrack.setText(""+track);
        editRoll.setText(""+roll);
        editHeight.setText(""+height);
        editSuspension.setText(""+suspension);
        editNonsense1.setText(""+nonsense1);
        editNonsense2.setText(""+nonsense2);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event){
        //处理后退键事件
        if(keyCode == KeyEvent.KEYCODE_BACK){
            storeEditData();
            exitToMainActivity();
        }
        return super.onKeyUp(keyCode, event);
    }

    private void storeEditData() {
        // TODO Auto-generated method stub
        mass = Double.parseDouble( editMass.getEditableText().toString() );
        sprung = Double.parseDouble( editSprung.getEditableText().toString() );
        track = Double.parseDouble( editTrack.getEditableText().toString() );
        roll = Double.parseDouble( editRoll.getEditableText().toString() );
        height = Double.parseDouble( editHeight.getEditableText().toString() );
        suspension = Double.parseDouble( editSuspension.getEditableText().toString() );
        nonsense1 = Double.parseDouble( editNonsense1.getEditableText().toString() );
        nonsense2 = Double.parseDouble( editNonsense2.getEditableText().toString() );
    }

    private void exitToMainActivity(){
        // TODO Auto-generated method stub
        Intent intent = new Intent(VehicleActivity.this, MainActivity.class);
        Bundle bundle = new Bundle();
        bundle.putDouble("mass", mass);
        bundle.putDouble("sprung", sprung);
        bundle.putDouble("track",track);
        bundle.putDouble("roll",roll);
        bundle.putDouble("height",height);
        bundle.putDouble("suspension",suspension);
        bundle.putDouble("nonsense1",nonsense1);
        bundle.putDouble("nonsense2",nonsense2);
        intent.putExtras(bundle);
        VehicleActivity.this.setResult(MainActivity.END_VEHICLE,intent);
        finish();
    }

}
