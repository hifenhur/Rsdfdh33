package models;

/**
 * Created by hifenhur on 06/02/15.
 */
public class GlobalParameters {
    private static GlobalParameters ourInstance = new GlobalParameters();

    public static GlobalParameters getInstance() {
        return ourInstance;
    }

    private GlobalParameters() {

    }

    public String defaultUrl = "http://torres.expark.com.br:3000";

    public String sellerUUID = null;



}
