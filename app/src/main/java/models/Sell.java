package models;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by hifenhur on 01/02/15.
 */
public class Sell implements Serializable {
    private int category;
    private Timestamp createdAt;
    private int status;
    private String value;
    private String content;
    private String uuid;
    private Timestamp limitAt;
    private String pdv;
    private String lote;
    private String nsu;
    private Integer vehicleType;


    public String toString() {
        String response = getCreatedAt();

        return response;
    }

    public Sell(int category,
                String uuid,
                Timestamp createdAt,
                String value,
                String content,
                Timestamp limitAt,
                String pdv,
                String lote,
                String nsu,
                String vehicleType) {

        this.category = category;
        this.uuid = uuid;
        this.createdAt = createdAt;
        this.value = value;
        this.content = content;
        this.limitAt = limitAt;
        this.pdv = pdv;
        this.lote = lote;
        this.nsu = nsu;
        this.vehicleType = vehicleType == "null" ? 0 : Integer.parseInt(vehicleType);

    }

    public String getType() {
        String sCategory;

        switch (category){
            case 1:
                sCategory =  "Ticket Avulso";
                break;
            case 2:
                sCategory =  "Credito Conta Cliente";
                break;
            case 3:
                sCategory = "Recarga de Cartão";
                break;
            case 4:
                sCategory = "Venda de Cartão";
                break;
            default:
                sCategory = "Modalidade Desconhecida";
                break;
        }
        return sCategory;

    }

    public String getContent() {
        return content;
    }


    public String getContentType() {
        String sCategory;

        switch (category){
            case 1:
                sCategory =  "Placa";
                break;
            case 2:
                sCategory =  "CPF";
                break;
            case 3:
                sCategory = "Cartao";
                break;
            case 4:
                sCategory = "Cartao";
                break;
            default:
                sCategory = "Cartao";
                break;
        }
        return sCategory;
    }

    public String getUuid() {
        return this.uuid;
    }

    public String getValue() {
        return this.value;
    }

    public String getCreatedAt() {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(createdAt);
    }

    public String getNsu() {
        return nsu;
    }

    public String getLimitAt() {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(limitAt);
    }

    public String getPdv() {
        return pdv;
    }

    public String getLote() {
        return lote;
    }

    public String getVehicleType(){
        if(vehicleType == 1){
            return "Carro";
        }else if (vehicleType == 2){
            return "Moto";
        }
        return "";
    }
}
