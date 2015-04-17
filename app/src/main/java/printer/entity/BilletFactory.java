package printer.entity;

import java.io.Serializable;

public class BilletFactory implements Serializable {
		
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String barcode;
	private String agentEnrollment;
	private String lineType;



	public BilletFactory(String lineType,String agentEnrollment){
		this.agentEnrollment=agentEnrollment;
		this.lineType=lineType;
		this.barcode=lineType.replace(".", "").replace(" ", "");
	}
	
	public String getLineType() {
		return lineType;
	}
	
	public boolean isBoletoBancario(){
		if(bank().startsWith("8")){
			return false;
		}else{
			return true;
		}
	}
	
	public String getTextoInformativoUsuario(){
		String texto = "";
		if(bank().equals("341")){
			texto = "PAG�VEL PREFERENCIALEMENTE NAS AG�NCIAS DO ITA�.";
		}else{
			texto = "PAG�VEL PREFERENCIALEMENTE NAS LOT�RICAS DA CAIXA.";
		}
		
		return texto;
	}
	public void setLineType(String lineType) {
		this.lineType = lineType;
		this.barcode=lineType.replace(".", "").replace(" ", "");
	}
	
	public String getAgentEnrollment() {
		return agentEnrollment;
	}
	
	public void setAgentEnrollment(String agentEnrollment) {
		this.agentEnrollment = agentEnrollment;
	}
	
	
	public String getBarcode(){
		int barcode = Integer.valueOf(bank());
        String barCodePrint = "";
        switch (barcode) {
			case 104:
				barCodePrint = getBarcodeCaixaEconomicaFederal();
				break;
			case 341:
				barCodePrint = getBarcodeItau();
				break;
			default:
				barCodePrint = getBarcodeArrecadacao();
				break;
		}
        
        return barCodePrint;
	}
	
	/*
	 * Metodo que obtem o codigo de barra impresso dos boletos de arrecadacao (Agua, Energia e etc)
	*/
	private String getBarcodeArrecadacao() {
		//Boleto Arrecadacao		
		String[] barcodes = this.lineType.split(" ");
		String barcode= barcodes[0] + barcodes[2]+ barcodes[4]+ barcodes[6];
		
		return barcode;
	}
	
	/*
	 * Metodo que obtem o codigo de barra impresso dos boletos da Caixa Economica Federal
	*/
	private String getBarcodeCaixaEconomicaFederal() {
		//Boleto Caixa
		String barcode =bank()+currency()+checkerDigit()+maturityDateBoletoBancario()+valueBoletoBancario()
				+barcodeNumber().substring(4, 9) + barcodeNumber().substring(10, 20) //Primeira parte campo livre
				+barcodeNumber().substring(21, 31);//Segunda parte campo livre.
		
		return barcode;
	}
	
	/*
	 * Metodo que obtem o codigo de barra impresso dos boletos do Itau.
	*/
	private String getBarcodeItau() {
		//Boleta Itau		
		String barcode=bank()+currency()+checkerDigit()+maturityDateBoletoBancario()+valueBoletoBancario()
				+barcodeNumber().substring(4, 7)+ourNumberBoletoItau()+cdOurNumberItau()+agencyParaBoletosItau()+accountItau()+cdAccountItau()+zeros();
		
		
		return barcode;
		
	}
	
	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	/**
	 * Codigo do banco digitos 1-3
	 */
	private String bank(){
		return barcodeNumber().substring(0, 3);
	}
	/**
	 * Moeda 4
	 */
	private String currency(){
		return ""+barcodeNumber().charAt(3);
	}
	/**
	 * Data de vencimento
	 */
	private String maturityDateBoletoArrecadacao(){
		return barcodeNumber().substring(20,23)+barcodeNumber().substring(24, 29);
	}
	
	/**
	 * Data de vencimento
	 */
	private String maturityDateBoletoBancario(){
		return barcodeNumber().substring(33,37);
	}

	
	/**
	 * Digito verificador geral
	 */
	private String checkerDigit(){
		return ""+barcodeNumber().charAt(32);
	}

	/**
	 * Valor
	 */
	private String valueBoletoArrecadacao(){
		return barcodeNumber().substring(12, 16);
	}
	
	
	/**
	 * Valor
	 */
	private String valueBoletoBancario(){
		return barcodeNumber().substring(37, 47);
	}
	
	private String cccBoletoCaixa(){
		return barcodeNumber().substring(4, 7);
	}
	
	private String cccBoletoItau(){
		return barcodeNumber().substring(4, 7);
	}
	
	
	/**
	 * Nosso numero parte 1
	 */
	private String ourNumberBoletoCaixa(){
		return barcodeNumber().substring(21, 30);
	}
	
	/**
	 * Nosso numero parte 1
	 */
	private String ourNumberBoletoItau(){
		return barcodeNumber().substring(7, 9)+barcodeNumber().substring(10, 16);
	}
	
	 /* 
	  * Nosso numero parte 1
	  */
	private String ourNumberBoletoArrecadacao(){
		return barcodeNumber().substring(29, 35);
	}

	private String cdOurNumberCaixa(){
		return ""+barcodeNumber().substring(30,31);
	}
	
	private String cdOurNumberItau(){
		return ""+barcodeNumber().substring(31, 32);
	}
	
	private String agencyParaBoletosCaixa(){
		return barcodeNumber().substring(12,15)+barcodeNumber().substring(16,17);
	}
	
	private String agencyParaBoletosItau(){
		return barcodeNumber().substring(17,20)+barcodeNumber().substring(21,22);
	}
	
	private String accountCaixa(){
		return barcodeNumber().substring(4, 9)+barcodeNumber().charAt(11);
	}
	
	private String accountItau(){
		return barcodeNumber().substring(22, 27);
	}
	
	private String cdAccountCaixa(){
		return ""+barcodeNumber().substring(11,12);
	}

	private String cdAccountItau(){
		return ""+barcodeNumber().substring(27,28);
	}

	private String barcodeNumber(){
		return barcode;
	}
	
	public String getMaturityDate(){
		if(bank().startsWith("8")){
			return Base.obterDataVencimentoBoletoFenabran(maturityDateBoletoArrecadacao());		
		}else{
			return Base.calculoDataVencimento(maturityDateBoletoBancario());
		}
		 
	}
	
	
	public String getValue(){
		if(bank().startsWith("8")){
			return Base.calculoValorNominal(valueBoletoArrecadacao());
		}else{
			return Base.calculoValorNominal(valueBoletoBancario());
		}
	}
	
	public String getLaunchNumber(){
		int banco = Integer.parseInt(bank());
		String launchNumber = "";
		switch (banco) {
			case 104:
				launchNumber = ourNumberBoletoCaixa();
				break;
			case 341:
				launchNumber = ourNumberBoletoItau();
				break;	
			default:
				launchNumber = ourNumberBoletoArrecadacao();
				break;
		}
		
		return launchNumber;
		
	}
	
	public String getBeneficiary(){
		if(bank().equals("104")){
			return agencyParaBoletosCaixa()+"/"+accountCaixa()+"-"+cdAccountCaixa();
		}else{
			return agencyParaBoletosItau()+"/"+accountItau()+"-"+cdAccountItau();
		}
		
	}
	
	public String getOurNumber(){
		if(bank().equals("104")){
			return barcodeNumber().substring(15,16)+barcodeNumber().substring(19,20)+"/"+ourNumberBoletoCaixa()+"-"+cdOurNumberCaixa();
		}else{
			return cccBoletoItau()+"/"+ourNumberBoletoItau()+"-"+cdOurNumberItau();
		}
		
	}
	
	private String zeros(){
		return barcodeNumber().substring(28,31);
	}
}
