package mad.testalize;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by baudson on 30/09/16.
 * Sert � utiliser le micro de l'appareil Android en enregistrant les donn�es brutes
 * Forme des don�nes: 11025Hz, une bande audio, 16bits / Features
 */

public class MicroInteract
{
    //Path du fichier une fois l'enregistrement termin�
    private String _filePath;

    //Param�tres enregistrement
    private AudioRecord _recorder;
    private int _bufferSize;
    private int _sampleRate = 44100;    // /!\ Seul 44100Hz fontionne sur tout device (autres valeurs possibles: 22050, 16000, et 11025)
    private int _channel = AudioFormat.CHANNEL_IN_MONO;
    private int _encoding = AudioFormat.ENCODING_PCM_16BIT;
    protected int _source = MediaRecorder.AudioSource.MIC;

    // Token de lecture
    private Boolean _inRec;


    public MicroInteract()
    {
        _bufferSize = AudioRecord.getMinBufferSize(_sampleRate, _channel, _encoding);
        _inRec = false;
        _filePath="";
    }


    /**
     * Permet d'enregistrer durant un temps donner en utilisant le microphone du t�l�phone
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


    /** D�marre un record Thread�
     * @param targetName : nom du fichier d'enregistrement
     */
    public void startRecord(String targetName)
    {
        //verification que l'on ne soit pas d�j� en lecture
        if(_inRec) return;
        _inRec=true;

        //Initialisation du recorder
        _recorder = new AudioRecord(_source, _sampleRate, _channel, _encoding, _bufferSize);
        _recorder.startRecording();

        //Lancement de la lecture dans un thread s�par�
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

    /** Convertir un tableau de shorts en bytes en d�coupant les short sur 2 bytes
     * Utilis� originellement par startRecord()
     * @param sTab : tableau de shorts � convertir
     * @return : tableau de bytes convertie
     */
    private byte[] shortTabToByteTab(short[] sTab)
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


    /** Permet de supprimer un fichier audio (ou autre)
     * @param path : chemain vers le fichier audio � supprimer
     * @return "true" si le ficheir audio existe et a �t� supprim�, "false" sinon
     */
    public Boolean removeAudioFile(String path)
    {
        File f = new File(path);
        if(!f.exists()) return false;
        return f.delete();
    }
}