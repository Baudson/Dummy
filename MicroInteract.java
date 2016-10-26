package mad.testalize;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * Created by Mad
 * Sert à utiliser le micro de l'appareil Android en enregistrant les données brutes
 */

public class MicroInteract
{
    //Path du fichier une fois l'enregistrement terminé
    protected String _filePath;

    //Paramètres enregistrement
    protected AudioRecord _recorder;
    protected int _bufferSize;
    protected int _sampleRate = 44100;    // /!\ Seul 44100Hz fontionne sur tout device (autres valeurs possibles: 22050, 16000, et 11025)
    protected int _channel = AudioFormat.CHANNEL_IN_MONO;
    protected int _encoding = AudioFormat.ENCODING_PCM_16BIT;
    protected int _source = MediaRecorder.AudioSource.MIC;

    // Token de lecture
    protected Boolean _inRec;


    /** Constructeur par défaut compatible théoriquement avec l'enssemble des devices.
     */
    public MicroInteract()
    {
        _bufferSize = AudioRecord.getMinBufferSize(_sampleRate, _channel, _encoding);
        _inRec = false;
        _filePath="";
    }


    /** Constructeur spécifiant l'enssemble des paramètres utiles
     * @param sampleRate : Valeur de l'échantillonage, valeurs possibles: 44100, 22050, 16000, 11025
     * @param channel : AudioFormat.CHANNEL_IN_MONO ou AudioFormat.CHANNEL_IN_STEREO
     * @param encodaing :  AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT ou AudioFormat.ENCODING_PCM_FLOAT
     * @param source : Voir https://lc.cx/oGqj pour plus d'info
     */
    public MicroInteract(int sampleRate, int channel, int encodaing, int source)
    {
        _sampleRate=sampleRate;
        _channel=channel;
        _encoding=encodaing;
        _source=source;
        _bufferSize = AudioRecord.getMinBufferSize(_sampleRate, _channel, _encoding);
        _inRec = false;
        _filePath="";
    }


    /**
     * Permet d'enregistrer durant un temps donner en utilisant le microphone du téléphone
     * @param timeInSeconde : temps en seconde durent lequel on doit enregistrer
     * @param targetRecordPath : path et nom du fichier audio
     * @return : path + nom du fichier d'enregistrement
     */
    public String recordDuring(int timeInSeconde, String targetRecordPath)
    {
        startRecord(targetRecordPath);
        try{Thread.sleep(timeInSeconde*1000);} catch (InterruptedException e) {e.printStackTrace();}
        return stopRecord();
    }


    /** Démarre un record Threadé
     * @param targetName : nom du fichier d'enregistrement
     */
    public void startRecord(String targetName)
    {
        //verification que l'on ne soit pas déjà en lecture
        if(_inRec) return;
        _inRec=true;

        //Initialisation du recorder
        _recorder = new AudioRecord(_source, _sampleRate, _channel, _encoding, _bufferSize);
        _recorder.startRecording();

        //Lancement de la lecture dans un thread séparé
        new Thread(new Runnable() {
            public void run()
            {
                short sData[] = new short[_bufferSize];
                FileOutputStream os = null;
                try{
                    os = new FileOutputStream(_filePath);
                    while(_inRec)
                    {
                        int readSize = _recorder.read(sData, 0, _bufferSize);
                        os.write(shortTabToByteTab(sData), 0, readSize*2);
                    }
                    os.close();
                }catch(Exception e){e.printStackTrace();}
            }
        }).start();
    }


    /** Stop le record courant et retourne le path du fichier en cours d'enregistrement.
     * @return une chaine vide si aucun record en cours, le path + nom du record final sinon.
     */
    public String stopRecord()
    {
        if(!_inRec) return "";
        _inRec=false;
        _recorder.stop();
        _recorder.release();
        _recorder = null;
        return _filePath;
    }

    /** Convertir un tableau de shorts en bytes en découpant les short sur 2 bytes
     * Utilisé originellement par startRecord()
     * @param sTab : tableau de shorts à convertir
     * @return : tableau de bytes convertie
     */
    protected byte[] shortTabToByteTab(short[] sTab)
    {
        int sTabSize = sTab.length;
        byte[] bTab = new byte[sTabSize*2];
        for (int i=0;i<sTabSize;i++)
        {
            bTab[i*2] = (byte) (sTab[i] & 0x00FF);
            bTab[(i*2)+1] = (byte) (sTab[i] >> 8);
            sTab[i] = 0;
        }
        return bTab;
    }

    /** Permet de convertir un fichier .raw enregistrer à 44100Hz vers 11025Hz exploitable par Alize (non-testé)
     * @param filePath : Path du fichier à convertire, celui-ci sera écrasé
     */
    protected void convertTo11025Hz(String filePath)
    {
        try {
            // Récupération du fichier en byte[]
            File f = new File(filePath);
            FileInputStream is = new FileInputStream(f);
            int l = (int) f.length();
            byte[] b = new byte[l];;
            ArrayList<Byte> r = new ArrayList<Byte>();
            is.read(b);
            is.close();

            // Suppresion de 3/4 des données
            for(int i=0;i<l;i+=2)
                if(i%8==0)
                {
                    r.add(b[i]);
                    r.add(b[i+1]);
                }

            // Ecriture des nouvelles données dans le fichier .raw source
            FileOutputStream os = new FileOutputStream(f);
            os.write(arrayListToTabByte(r), 0, r.size());
            os.close();
        } catch(Exception e){e.printStackTrace();}
    }

    //N'existe pas de méthode standard pour convertire une ArrayList<byte> en byte[]
    protected byte[] arrayListToTabByte(ArrayList<Byte> lb)
    {
        byte[] r = new byte[lb.size()];
        for(int i=0;i<r.length;i++) r[i]=lb.get(i);
        return r;
    }


    /** Permet de supprimer un fichier audio (ou autre)
     * @param path : chemain vers le fichier audio à supprimer
     * @return "true" si le ficheir audio existe et a été supprimé, "false" sinon
     */
    public Boolean removeAudioFile(String path)
    {
        File f = new File(path);
        if(!f.exists()) return false;
        return f.delete();
    }
}

/*
    (\ _ /)
    (='.'=) {I'm not Mad.)
    (")-(")
*/
