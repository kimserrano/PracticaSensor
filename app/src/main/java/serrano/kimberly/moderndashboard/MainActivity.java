package serrano.kimberly.moderndashboard;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    CardView clothingCard;
    SwitchCompat switchMode;
    boolean nightMode;
    SensorManager sensorManager;
    Sensor lightSensor;

    private static final String LIGHT_THRESHOLD_KEY = "light_threshold";
    private static final float DEFAULT_LIGHT_THRESHOLD = 3.6f;
    private static final float LIGHT_DAY_THRESHOLD = 60.0f;
    private static final float LIGHT_MARGIN = 10.0f; // Margen para transiciones suaves

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switchMode = findViewById(R.id.swithMode);
        clothingCard = findViewById(R.id.clothingCard);

        switchMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nightMode) {
                    setDayMode();
                } else {
                    setNightMode();
                }
            }
        });

        clothingCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ClothingActivity.class);
                startActivity(intent);
            }
        });

        // Registrar el Sensor de Luz (si todavía lo estás utilizando)
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (lightSensor != null) {
            sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Cargar el umbral de luz predeterminado si no existe
        loadAndUpdateLightThreshold();
    }

    private final SensorEventListener lightSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float lightLevel = event.values[0];
            Log.d("LightSensor", "Light level: " + lightLevel);

            // Obtener el umbral de luz desde SharedPreferences y actualizarlo
            float threshold = loadAndUpdateLightThreshold();

            // Cambiar automáticamente entre los modos basado en el valor de luz
            if (lightLevel <= threshold && !nightMode) {
                Log.d("LightSensor", "Activating Night Mode");
                setNightMode();
            } else if (lightLevel >= LIGHT_DAY_THRESHOLD && nightMode) {
                Log.d("LightSensor", "Activating Day Mode");
                setDayMode();
            }

            // Log para verificar el valor del umbral de luz después de cambiar
            Log.d("LightThreshold", "Umbral de luz actualizado: " + threshold);

            // Si el lightLevel es mayor que el umbral guardado, actualizar el umbral
            if (lightLevel > threshold + LIGHT_MARGIN) {
                saveLightThreshold(lightLevel);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // No necesitamos hacer nada aquí
        }
    };

    // Método para cambiar a modo noche
    private void setNightMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        nightMode = true;
        switchMode.setChecked(true);
    }

    // Método para cambiar a modo día
    private void setDayMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        nightMode = false;
        switchMode.setChecked(false);
    }

    // Método para guardar el umbral de luz
    private void saveLightThreshold(float threshold) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(LIGHT_THRESHOLD_KEY, threshold);
        editor.apply();
    }

    // Método para cargar y actualizar el umbral de luz
    private float loadAndUpdateLightThreshold() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        float threshold = sharedPreferences.getFloat(LIGHT_THRESHOLD_KEY, DEFAULT_LIGHT_THRESHOLD);
        return threshold;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lightSensor != null) {
            sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (lightSensor != null) {
            sensorManager.unregisterListener(lightSensorListener);
        }
    }
}
