package cn.leisik.teleportation;

import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class BankData implements Serializable {
    private static transient final long serialVersionUID = -1681012206529286330L;
    private final static String FILE_NAME = "serverBank";
    private final Map<String, Integer> serverBank;

    public BankData(Map<String, Integer> serverBank) {
        this.serverBank = serverBank;
    }

    public static BankData loadData() {
        try {
            File f = new File(FILE_NAME);
            // create a brand new data when first enabled
            if (!f.exists()) {
                return new BankData(new HashMap<String, Integer>());
            } else {
                BukkitObjectInputStream in = new BukkitObjectInputStream(new GZIPInputStream(new FileInputStream(f)));
                BankData data = (BankData) in.readObject();
                in.close();
                return data;
            }
        } catch (ClassNotFoundException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public boolean saveData() {
        try {
            BukkitObjectOutputStream out = new BukkitObjectOutputStream(new GZIPOutputStream(new FileOutputStream(FILE_NAME)));
            out.writeObject(this);
            out.close();
            return true;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    public Map<String, Integer> getServerBank() {
        return this.serverBank;
    }

}
