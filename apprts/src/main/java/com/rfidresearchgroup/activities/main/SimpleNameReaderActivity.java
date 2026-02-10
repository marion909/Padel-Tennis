package com.rfidresearchgroup.activities.main;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.Nullable;

import com.rfidresearchgroup.rfidtools.R;
import com.rfidresearchgroup.common.widget.ToastUtil;
import com.rfidresearchgroup.natives.SpclMf;
import com.rfidresearchgroup.common.util.HexUtil;
import com.rfidresearchgroup.mifare.MifareClassicUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public class SimpleNameReaderActivity extends BaseActivity {

    private EditText edtName;
    private Button btnWrite;
    private Button btnRead;
    private TextView txtResult;
    private SpclMf spclMf;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_simple_name_reader);

        initViews();
        initActions();
        
        // Get PN532 interface
        spclMf = SpclMf.get();
    }

    private void initViews() {
        edtName = findViewById(R.id.edtName);
        btnWrite = findViewById(R.id.btnWriteName);
        btnRead = findViewById(R.id.btnReadName);
        txtResult = findViewById(R.id.txtResult);
    }

    private void initActions() {
        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = edtName.getText().toString();
                if (name.isEmpty()) {
                    ToastUtil.show(context, "Bitte Namen eingeben", false);
                    return;
                }
                writeName(name);
            }
        });

        btnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readName();
            }
        });
    }

    private void writeName(String name) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    showResult("🔍 Suche NFC-Tag...\n\nBitte Tag an Leser halten");
                    
                    // Scan for tag
                    if (!spclMf.scanning()) {
                        showResult("❌ Kein Tag gefunden!\n\nBitte Tag an Leser halten und erneut versuchen.");
                        return;
                    }
                    
                    showResult("📡 Verbinde mit Tag...");
                    
                    // Connect to tag
                    if (!spclMf.connect()) {
                        showResult("❌ Verbindung fehlgeschlagen!\n\nBitte erneut versuchen.");
                        return;
                    }
                    
                    // Generate UUID for this player
                    String uuid = UUID.randomUUID().toString();
                    
                    // Convert name to bytes (max 16 bytes per block)
                    byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
                    
                    // Prepare name data (pad to 16 bytes)
                    byte[] nameData = new byte[16];
                    Arrays.fill(nameData, (byte) 0);
                    System.arraycopy(nameBytes, 0, nameData, 0, Math.min(nameBytes.length, 16));
                    
                    // Convert UUID to bytes (max 16 bytes for first part)
                    byte[] uuidBytes = uuid.getBytes(StandardCharsets.UTF_8);
                    byte[] uuidData1 = new byte[16];
                    Arrays.fill(uuidData1, (byte) 0);
                    System.arraycopy(uuidBytes, 0, uuidData1, 0, Math.min(uuidBytes.length, 16));
                    
                    // Second part of UUID (if needed)
                    byte[] uuidData2 = new byte[16];
                    Arrays.fill(uuidData2, (byte) 0);
                    if (uuidBytes.length > 16) {
                        System.arraycopy(uuidBytes, 16, uuidData2, 0, Math.min(uuidBytes.length - 16, 16));
                    }
                    
                    // Default MIFARE key
                    byte[] defaultKey = HexUtil.hexStringToByteArray("FFFFFFFFFFFF");
                    
                    showResult("🔐 Authentifiziere...");
                    
                    // Authenticate sector 1 (Blocks 4-7) with key A
                    int sector = MifareClassicUtils.blockToSector(4);
                    if (!spclMf.authA(sector, defaultKey)) {
                        showResult("❌ Authentifizierung fehlgeschlagen!\n\nTag ist möglicherweise gesperrt.");
                        return;
                    }
                    
                    showResult("💾 Schreibe Spielerdaten...\n\n📝 Name...\n🆔 UUID (1/2)...\n🆔 UUID (2/2)...");
                    
                    // Write name to block 4
                    if (!spclMf.write(4, nameData)) {
                        showResult("❌ Schreiben fehlgeschlagen!\n\nBitte erneut versuchen.");
                        return;
                    }
                    
                    // Write first part of UUID to block 5
                    if (!spclMf.write(5, uuidData1)) {
                        showResult("❌ UUID-Schreiben fehlgeschlagen (1/2)\n\nBitte erneut versuchen.");
                        return;
                    }
                    
                    // Write second part of UUID to block 6
                    if (!spclMf.write(6, uuidData2)) {
                        showResult("❌ UUID-Schreiben fehlgeschlagen (2/2)\n\nBitte erneut versuchen.");
                        return;
                    }
                    
                    showResult("✅ Spieler erfolgreich registriert!\n\n👤 Name: " + name + "\n🆔 UUID: " + uuid + "\n\n✓ Tag ist bereit für Padel Tennis!");
                    
                } catch (Exception e) {
                    showResult("❌ Fehler: " + e.getMessage() + "\n\nBitte erneut versuchen.");
                }
            }
        }).start();
    }

    private void readName() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    showResult("🔍 Suche NFC-Tag...\n\nBitte Tag an Leser halten");
                    
                    // Scan for tag
                    if (!spclMf.scanning()) {
                        showResult("❌ Kein Tag gefunden!\n\nBitte Tag an Leser halten und erneut versuchen.");
                        return;
                    }
                    
                    showResult("📡 Verbinde mit Tag...");
                    
                    // Connect to tag
                    if (!spclMf.connect()) {
                        showResult("❌ Verbindung fehlgeschlagen!\n\nBitte erneut versuchen.");
                        return;
                    }
                    
                    showResult("🔐 Authentifiziere...");
                    
                    // Default MIFARE key
                    byte[] defaultKey = HexUtil.hexStringToByteArray("FFFFFFFFFFFF");
                    
                    // Authenticate sector 1 (Blocks 4-7) with key A
                    int sector = MifareClassicUtils.blockToSector(4);
                    if (!spclMf.authA(sector, defaultKey)) {
                        showResult("❌ Authentifizierung fehlgeschlagen!\n\nTag ist möglicherweise gesperrt.");
                        return;
                    }
                    
                    showResult("📖 Lese Spielerdaten...\n\n📝 Name...\n🆔 UUID...");
                    
                    // Read name from block 4
                    byte[] nameData = spclMf.read(4);
                    
                    if (nameData == null || nameData.length < 16) {
                        showResult("❌ Lesen fehlgeschlagen!\n\nTag enthält keine Daten.");
                        return;
                    }
                    
                    // Read UUID part 1 from block 5
                    byte[] uuidData1 = spclMf.read(5);
                    if (uuidData1 == null || uuidData1.length < 16) {
                        showResult("❌ UUID-Lesen fehlgeschlagen (1/2)\n\nTag ist beschädigt.");
                        return;
                    }
                    
                    // Read UUID part 2 from block 6
                    byte[] uuidData2 = spclMf.read(6);
                    if (uuidData2 == null || uuidData2.length < 16) {
                        showResult("❌ UUID-Lesen fehlgeschlagen (2/2)\n\nTag ist beschädigt.");
                        return;
                    }
                    
                    // Convert bytes to string and remove padding
                    String name = new String(nameData, 0, 16, StandardCharsets.UTF_8).trim();
                    name = name.replaceAll("\\x00", "");
                    
                    // Combine UUID parts
                    byte[] uuidBytes = new byte[32];
                    System.arraycopy(uuidData1, 0, uuidBytes, 0, 16);
                    System.arraycopy(uuidData2, 0, uuidBytes, 16, 16);
                    String uuid = new String(uuidBytes, StandardCharsets.UTF_8).trim();
                    uuid = uuid.replaceAll("\\x00", "");
                    
                    if (name.isEmpty()) {
                        showResult("⚠️ Kein Spieler registriert\n\nDieser Tag ist leer oder wurde noch nicht beschrieben.");
                    } else {
                        showResult("✅ Spieler gefunden!\n\n👤 Name: " + name + "\n🆔 UUID: " + uuid + "\n\n✓ Dieser Spieler ist registriert!");
                    }
                    
                } catch (Exception e) {
                    showResult("❌ Fehler: " + e.getMessage() + "\n\nBitte erneut versuchen.");
                }
            }
        }).start();
    }

    private void showResult(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtResult.setText(message);
            }
        });
    }
}
