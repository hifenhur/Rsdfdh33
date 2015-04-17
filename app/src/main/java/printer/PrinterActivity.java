package printer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.datecs.api.printer.Printer;
import com.datecs.api.printer.PrinterInformation;
import com.datecs.api.printer.ProtocolAdapter;

import br.com.expark.pdvdesk.SalesActivity;
import models.Sell;
import printer.entity.Base;
import printer.entity.BilletFactory;
import printer.network.PrinterServer;
import printer.network.PrinterServerListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.UUID;

import br.com.expark.pdvdesk.R;
import util.Util;


public class PrinterActivity extends Activity {
    // Debug
    private static final String LOG_TAG = "PrinterSample";
    private static final boolean DEBUG = true;

    Sell mSell;
    
    // Request to get the bluetooth device
    private static final int REQUEST_GET_DEVICE = 0; 
    
    // Request to get the bluetooth device
    private static final int DEFAULT_NETWORK_PORT = 9100; 
    
    //Servir� para informar sucesso a thread de impress�o
    @SuppressWarnings("unused")
	private static boolean WAS_PRINTED = false;
    
    //Para evitar mensagens repetidas
    private static int contador = 0;
    
	// The listener for all printer events
	private final ProtocolAdapter.ChannelListener mChannelListener = new ProtocolAdapter.ChannelListener() {        
        @Override

        public void onReadEncryptedCard() {
            toast(getString(R.string.msg_read_encrypted_card));
        }
        
        @Override
        public void onReadCard() {}
        
        @Override
        public void onReadBarcode() {}
        
        @Override
        public void onPaperReady(boolean state) {
        	if (state) {
                toast(getString(R.string.msg_paper_ready));
               
            } else {
            	if(contador < 1){
            		toast(getString(R.string.msg_no_paper));
            		
            		contador++;
            	}
            }
        }
        
        @Override
        public void onOverHeated(boolean state) {
            if (state) {
                toast(getString(R.string.msg_overheated));
            }
        }
               
        @Override
        public void onLowBattery(boolean state) {
            if (state) {
                toast(getString(R.string.msg_low_battery));
            }
        }
    }; 
    
    // Member variables
	private Printer mPrinter;
	private ProtocolAdapter mProtocolAdapter;
	private PrinterInformation mPrinterInfo;
	private BluetoothSocket mBluetoothSocket;
	private PrinterServer mPrinterServer;
	private Socket mPrinterSocket;
	private boolean mRestart;

	//private boolean hasPaper;
	private Base mBase;
	//private String barcode;
	ProgressDialog progress;
	private boolean concordia;
	private BilletFactory billetFactory;
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        setContentView(R.layout.activity_printer);
        setTitle(getString(R.string.app_name) + " Mobile Printer"); 
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mSell = (Sell) this.getIntent().getSerializableExtra("sell");

        final Context ctx = this;
        
        findViewById(R.id.print_page).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	try {
					if(mPrinter.getStatus()!=4)
					{
						printButtonContinue();
					}
					else{
                        Util.aviso("Aviso", "Sem papel", PrinterActivity.this);}
				} catch (IOException e) {
					Util.aviso("Aviso", "Ocorreu um erro inesperado", PrinterActivity.this);
				}

            }           
        });     
        //Bot�o para exit da aplica��o, poder� tamb�m ser feito no onBackPressed(), por�m sem aviso de saida.
        findViewById(R.id.exit).setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				try{
					mPrinter.turnOff();
				}
				catch(Exception ex){
					
				}
				Intent intent = new Intent(ctx, SalesActivity.class);
                startActivity(intent);

	    		finish();
			}
        	
        });
        
       
        
        mRestart = true;
        waitForConnection();
    }

    @Override
	protected void onDestroy() {
        super.onDestroy();
        mRestart = false;        
        contador = 0;
        closeActiveConnection();
	}	

    /*@Override
	public boolean onCreateOptionsMenu(Menu menu){
    	menu.add(0,1,0,"Sair");
		return super.onCreateOptionsMenu(menu);
    }*/
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        final Context ctx = this.getApplicationContext();
    	switch (item.getItemId()) {
		//Caso 1 - Exit
		case 1:
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
			        switch (which){
				        case DialogInterface.BUTTON_POSITIVE:
				        	Intent intent = new Intent(ctx, SalesActivity.class);
                            startActivity(intent);
				    		finish();
				            break;
				        default:
				        	break;
			        }
			    }
			};
			//Mensagem que desaparece automaticamente
			AlertDialog.Builder builder = new AlertDialog.Builder(PrinterActivity.this);
	        builder.setTitle(R.string.information);
	        builder.setCancelable(false);
	        builder.setMessage(R.string.ask_to_exit);
	        builder.setPositiveButton(R.string.yes, dialogClickListener);
	        builder.setNegativeButton(R.string.no, null);
	        final AlertDialog dlg = builder.create();
	        dlg.show();
			break;
    	}
		return mRestart;    
    }
   
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == REQUEST_GET_DEVICE) {
            if (resultCode == DeviceListActivity.RESULT_OK) {   
            	String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            	//address = "192.168.11.136:9100";
            	if (BluetoothAdapter.checkBluetoothAddress(address)) {
            		establishBluetoothConnection(address);
            	} else {
            		establishNetworkConnection(address);
            	}
            } else if (resultCode == RESULT_CANCELED) {
                //intent lancao ao cancelar
                finish();
            } else {
                finish();
            }
        }
    }
    
	@Override
	public void onBackPressed(){
        final Context ctx = this.getApplicationContext();
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
			        case DialogInterface.BUTTON_POSITIVE:
			        	try{
							Intent intent = new Intent(ctx, SalesActivity.class);
                            startActivity(intent);
						}
						catch(Exception ex){
							
						}
			        	//Prepara um intent para ser disparado quando o usu�rio clicar no bot�o voltar

			            break;
			        default:
			        	break;
		        }
		    }
		};
		//Mensagem que desaparece automaticamente
		AlertDialog.Builder builder = new AlertDialog.Builder(PrinterActivity.this);
        builder.setTitle("Informa��o");
        builder.setCancelable(false);
        builder.setMessage("Voc� deseja sair?");
        builder.setPositiveButton("Sim", dialogClickListener);
        builder.setNegativeButton("N�o", null);
        final AlertDialog dlg = builder.create();
        dlg.show();
		return;
	}
    
    private void toast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void dialog(final int iconResId, final String title, final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(PrinterActivity.this);
                builder.setIcon(iconResId);
                builder.setTitle(title);
                builder.setMessage(msg);
                
                AlertDialog dlg = builder.create();
                dlg.show();             
            }           
        });             
    }
    
    private void error(final String text, boolean resetConnection) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	if(!text.contains("Operation") && !text.contains("object")){
            		Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            	}
            	else{
            		//Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            	}
            }           
        });
        
        if (resetConnection) {
            waitForConnection();
        }
    }
    
    private void doJob(final Runnable job, final int resId) {
        // Start the job from main thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
             // Progress dialog title
                String title = getString(R.string.title_please_wait);
                // Progress dialog message
                String text = getString(resId);
                // Progress dialog available due job execution
                final ProgressDialog dialog = ProgressDialog.show(PrinterActivity.this, title, text);
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            job.run();
                        } finally {
                           dialog.dismiss();
                        }
                    }
                }).start();
            }
        });
    }
    
    protected void initPrinter(InputStream inputStream, OutputStream outputStream) throws IOException {
        mProtocolAdapter = new ProtocolAdapter(inputStream, outputStream);
       
        if (mProtocolAdapter.isProtocolEnabled()) {
            final ProtocolAdapter.Channel channel = mProtocolAdapter.getChannel(ProtocolAdapter.CHANNEL_PRINTER);
            channel.setListener(mChannelListener);
            // Create new event pulling thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        
                        try {
                            channel.pullEvent();
                        } catch (IOException e) {
                        	e.printStackTrace();
                            error(e.getMessage(), mRestart);
                            break;
                        }
                    }
                }
            }).start();
            mPrinter = new Printer(channel.getInputStream(), channel.getOutputStream());
        } else {
            mPrinter = new Printer(mProtocolAdapter.getRawInputStream(), mProtocolAdapter.getRawOutputStream());
        }
        
        mPrinterInfo = mPrinter.getInformation();
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((ImageView)findViewById(R.id.icon)).setImageResource(R.drawable.icon);
                ((TextView)findViewById(R.id.name)).setText(mPrinterInfo.getName());
            }
        });
    }
    
    public synchronized void waitForConnection() {
        closeActiveConnection();
        
        // Show dialog to select a Bluetooth device. 
        Intent intent = new Intent(this, DeviceListActivity.class);
        //intent.putExtra("usuario", user);
        startActivityForResult(intent, REQUEST_GET_DEVICE);
        
        // Start server to listen for network connection.
        try {
            mPrinterServer = new PrinterServer(new PrinterServerListener() {                
                @Override
                public void onConnect(Socket socket) {
                    if (DEBUG) Log.d(LOG_TAG, "Accept connection from " + socket.getRemoteSocketAddress().toString());
                    
                    // Close Bluetooth selection dialog
                    finishActivity(REQUEST_GET_DEVICE);                    
                    
                    mPrinterSocket = socket;
                    try {
                        InputStream in = socket.getInputStream();
                        OutputStream out = socket.getOutputStream();
                        initPrinter(in, out);
                    } catch (IOException e) {
                    	e.printStackTrace();
                        error(getString(R.string.msg_failed_to_init) + ". " + e.getMessage(), mRestart);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void establishBluetoothConnection(final String address) {
        closePrinterServer();

        doJob(new Runnable() {
            @Override
            public void run() {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice device = adapter.getRemoteDevice(address);
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                InputStream in = null;
                OutputStream out = null;

                adapter.cancelDiscovery();

                try {
                    if (DEBUG) Log.d(LOG_TAG, "Connect to " + device.getName());
                    mBluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
                    mBluetoothSocket.connect();
                    in = mBluetoothSocket.getInputStream();
                    out = mBluetoothSocket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                    error(getString(R.string.msg_failed_to_connect) + ". " +  e.getMessage(), mRestart);
                    return;
                }

                try {
                    initPrinter(in, out);
                } catch (IOException e) {
                    e.printStackTrace();
                    error(getString(R.string.msg_failed_to_init) + ". " +  e.getMessage(), mRestart);
                    return;
                }
            }
        }, R.string.msg_connecting);
    }
    
    private void establishNetworkConnection(final String address) {
    	closePrinterServer();
        
        doJob(new Runnable() {
            @Override
            public void run() {            	
            	Socket s = null;
            	try {
            		String[] url = address.split(":");
            		int port = DEFAULT_NETWORK_PORT;
            		
            		try {
            			if (url.length > 1)  {
            				port = Integer.parseInt(url[1]);
            			}
            		} catch (NumberFormatException e) { }
            		
            		s = new Socket(url[0], port);
            		s.setKeepAlive(true);
                    s.setTcpNoDelay(true);
	            } catch (UnknownHostException e) {
	            	error(getString(R.string.msg_failed_to_connect) + ". " +  e.getMessage(), mRestart);
                    return;
	            } catch (IOException e) {
	            	error(getString(R.string.msg_failed_to_connect) + ". " +  e.getMessage(), mRestart);
                    return;
	            }            
            	
                InputStream in = null;
                OutputStream out = null;
                
                try {
                    if (DEBUG) Log.d(LOG_TAG, "Connect to " + address);
                    mPrinterSocket = s;                    
                    in = mPrinterSocket.getInputStream();
                    out = mPrinterSocket.getOutputStream();                                        
                } catch (IOException e) {
                    error(getString(R.string.msg_failed_to_connect) + ". " +  e.getMessage(), mRestart);
                    return;
                }                                  
                
                try {
                    initPrinter(in, out);
                } catch (IOException e) {
                    error(getString(R.string.msg_failed_to_init) + ". " +  e.getMessage(), mRestart);
                    return;
                }
            }
        }, R.string.msg_connecting); 
    }
    
    private synchronized void closeBlutoothConnection() {        
        // Close Bluetooth connection 
        BluetoothSocket s = mBluetoothSocket;
        mBluetoothSocket = null;
        if (s != null) {
            if (DEBUG) Log.d(LOG_TAG, "Close Blutooth socket");
            try {
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }        
    }
    
    private synchronized void closeNetworkConnection() {
        // Close network connection
        Socket s = mPrinterSocket;
        mPrinterSocket = null;
        if (s != null) {
            if (DEBUG) Log.d(LOG_TAG, "Close Network socket");
            try {
                s.shutdownInput();
                s.shutdownOutput();
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }            
        }
    }
    
    private synchronized void closePrinterServer() {
    	closeNetworkConnection();
    	
        // Close network server
        PrinterServer ps = mPrinterServer;
        mPrinterServer = null;
        if (ps != null) {
            if (DEBUG) Log.d(LOG_TAG, "Close Network server");
            try {
                ps.close();
            } catch (IOException e) {
                e.printStackTrace();
            }            
        }     
    }
    
    private synchronized void closePrinterConnection() {
        if (mPrinter != null) {
            mPrinter.release();
        }
        
        if (mProtocolAdapter != null) {
            mProtocolAdapter.release();
        }
    }
    
    private synchronized void closeActiveConnection() {
        closePrinterConnection();
        closeBlutoothConnection();
        closeNetworkConnection();  
        closePrinterServer();
    }
    public void printButtonContinue()
    {
    		printPage();

    	
    }
    /* Imprime o boleto */
	private void printPage() {
	    doJob(new Runnable() {
            @Override
            public void run() {
            	
            	printBoleto(); 
            	
        	     
            }
	    }, R.string.msg_printing_page);
    }
	public void printBoleto()
	{
		if (mPrinterInfo == null || !mPrinterInfo.isPageModeSupported()) {
	        dialog(R.drawable.page, 
	                getString(R.string.title_warning), 
	                getString(R.string.msg_unsupport_page_mode));
	        return;
	    }
	      
	    	  printPaymentComprovant();
	      
		
	}
	private void printPaymentComprovant()
	{
		 if (DEBUG) Log.d(LOG_TAG, "Print Page");
         try {
			
			printHeader();


			printBody();

		} catch (IOException e) {

             e.printStackTrace();
         }
		 catch (NameNotFoundException e) {

			e.printStackTrace();
		}
         
	}
	private void printHeader() throws IOException
	{
		mPrinter.reset();
		mPrinter.selectPageMode();

		mPrinter.setPageRegion(0, 0, 400, 128, Printer.PAGE_LEFT);
        mPrinter.setPageXY(0, 0);
        //Logo de concordia
        Base.printLogoPatrocinio(getApplicationContext(), mPrinter);
        //mPrinter.drawPageFrame(0, 0, 80, 80, Printer.FILL_WHITE, 1);
        /* Linha morta para evitar cortar o topo das letras da proxima linha
         * Cabe�alho cabe 34 caracteres
         */
        /*Quebra de linha, afasta o cabe�alho do texto subsequente
         * Novo cabe�alho para texto pr� formatado
         */
        
        mPrinter.printPage();
        mPrinter.flush();//m
        mPrinter.reset();
        
        
	}
	private void printBody() throws IOException, NameNotFoundException
	{
        mPrinter.feedPaper(110);

		mPrinter.selectStandardMode();
		StringBuilder sb = new StringBuilder();

        mPrinter.printTaggedText("{reset}{s}{br}","UTF-8");

        mPrinter.printTaggedText("{reset}{s}{b}Tipo da Venda:{/b} " + mSell.getType() + "{br}", "UTF-8");

        mPrinter.printTaggedText("{reset}{s}{b}"+mSell.getContentType()+"{/b}: "+ mSell.getContent() +"{br}","UTF-8");


        mPrinter.printTaggedText("{reset}{s}{b}Valor:{/b} " + mSell.getValue() + "{br}", "UTF-8");


        mPrinter.printTaggedText("{reset}{s}{b}data:{/b} " + mSell.getCreatedAt() + "{br}", "UTF-8");

        if (mSell.getContentType() =="Placa"){
            mPrinter.printTaggedText("{reset}{s}{b}hora limite:{/b} " + mSell.getLimitAt() + "{br}", "UTF-8");

            mPrinter.printTaggedText("{reset}{s}{b}lote:{/b} " + mSell.getLote() + "{br}", "UTF-8");

            mPrinter.printTaggedText("{reset}{s}{b}NSU:{/b} " + mSell.getNsu() + "{br}", "UTF-8");

            mPrinter.printTaggedText("{reset}{s}{b}veiculo:{/b} " + mSell.getVehicleType() + "{br}", "UTF-8");

        }

        mPrinter.printTaggedText("{reset}{s}{b}pdv:{/b} " + mSell.getPdv() + "{br}", "UTF-8");


        mPrinter.printPage();
        mPrinter.flush();
        mPrinter.selectStandardMode();
        WAS_PRINTED = true;
        mPrinter.feedPaper(110);
        mBluetoothSocket.close();

        
	}
	
	
	private void printBodyPatrocinioBoletoArrecadacao() throws IOException, NameNotFoundException
	{
		mPrinter.selectStandardMode();
		StringBuilder sb = new StringBuilder();
        sb.append("{reset}{br}{br}");
        sb.append("{reset}{b}{s}Vencimento "+billetFactory.getMaturityDate()+"{br}");
        sb.append("{reset}{s}Regularizar at� 5 (cinco) dias �teis, conf{br}");
        sb.append("{reset}{s}orme instru��es do boleto, onde ser� pago{br}");
        sb.append("{reset}{s}o valor de 5(UFEMG Municipal). Ap�s os cin{br}");
        sb.append("{reset}{s}co dias �teis, se a presente notifica��o n{br}");
        sb.append("{reset}{s}�o for paga, ser� convertida em infra��o d{br}");
        sb.append("{reset}{s}e tr�nsito, de acordo com o amparo legal,{br}");
        sb.append("{reset}{s}ARTIGO, 181 - XVII do CTB.{br}{br}");
        sb.append("{reset}{s}Sr. usu�rio: na data {b}"+Base.retorneDataAtual()+"{/b} seu ve�cul{br}");
        sb.append("{reset}{s}o foi notificado as {b}"+Base.retorneHoraAtual()+"{/b}, de acordo{br}");
        sb.append("{reset}{s}com as penalidades previstas na legisla��o{br}");
        sb.append("{reset}{s}de tr�nsito em virtude de irregularidade c{br}");
        sb.append("{reset}{s}ometida no controle do estacionamento rota{br}");
        sb.append("{reset}{s}tivo.{br}{br}");
        //sb.append("{reset}[x] "+Util.obterTypeInfractionBy((int)PrinterActivity.this.infracao.getInfracao())+"{br}{br}{br}");
        //sb.append("{reset}{s}IDENTIFICA��O, VE�CULO E LOCAL ESTACIONADO{br}");
        //sb.append("{reset}{s}Placa: {/s}"+PrinterActivity.this.infracao.getPlaca()+" | {s}"+PrinterActivity.this.infracao.getCidadeP()+" | "+PrinterActivity.this.infracao.getEstadoP()+"{br}");
        //sb.append("{reset}{s}Rua: "+PrinterActivity.this.infracao.getRua()+"  N�"+PrinterActivity.this.infracao.getNumero()+"{br}");
        //sb.append("{reset}{s}Marca/Modelo: "+PrinterActivity.this.infracao.getMarca()+"/"+PrinterActivity.this.infracao.getModelo()+"{br}{br}");
        sb.append("{reset}{center}N� AGENTE{br}");
        sb.append("{reset}{center}"+billetFactory.getAgentEnrollment()+"{br}{br}");
        sb.append("{reset}{center}____________________________{br}");
        sb.append("{reset}{center}{s}Agente de Autoridade de Tr�nsito Municipal{br}{br}");
        mPrinter.printTaggedText(sb.toString(),"ISO-8859-1");
        mPrinter.printText("{reset}"+""+"{br}");
        mPrinter.flush();//m
        mPrinter.reset();
        mPrinter.selectPageMode();
        /***
         * Terceiro documento
         * Espa�amento entre linhas: 27
         * Espa�amento entre itens(cabe�alho + texto):16
		 * Espa�amento entre o cabe�alho e o form: 980
         */
        mPrinter.setPageRegion(0, 960, 400, 1460, Printer.PAGE_TOP);
            
        
        //Primeira linha
        String versionName = String.valueOf(this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode);
        mPrinter.setPageXY(11, 24);            
        mPrinter.printTaggedText("{reset}{s}Vers�o: "+versionName+"{br}","ISO-8859-1");
        //Segunda linha
        mPrinter.setPageXY(11, 45);            
        mPrinter.printTaggedText("{reset}{s}Vencimento{br}","ISO-8859-1");
        mPrinter.setPageXY(11, 60);
        mPrinter.printTaggedText("{reset}"+billetFactory.getMaturityDate()+"{br}","ISO-8859-1");     
        //Terceira linha
        mPrinter.setPageXY(11, 130);            
        mPrinter.printTaggedText("{reset}{s}(=) Valor Documento{br}","ISO-8859-1");
        mPrinter.setPageXY(11, 146);
        mPrinter.printTaggedText("{reset}{left}        "+billetFactory.getValue()+"{br}","ISO-8859-1");
        //Oitava linha
        mPrinter.setPageXY(11, 252); //297           
        mPrinter.printTaggedText("{reset}{s}Placa:{br}","ISO-8859-1");
        mPrinter.setPageXY(11, 268);//316
        //mPrinter.printTaggedText("{reset}"+PrinterActivity.this.infracao.getPlaca()+"{br}","ISO-8859-1");
        mPrinter.setPageXY(11, 342);
        mPrinter.printTaggedText("{reset}{s}Munic�pio:{br}","ISO-8859-1");
        mPrinter.setPageXY(11, 362);
        //mPrinter.printTaggedText("{reset}{s}"+PrinterActivity.this.infracao.getCidadeP()+" "+PrinterActivity.this.infracao.getEstadoP()+"{br}","ISO-8859-1");
       
        /* Parte do meio */
        mPrinter.setPageXY(263, 24);
        mPrinter.printTaggedText("{reset}{s}Outras Informa��es{br}","ISO-8859-1");
        /* Primeira linha */
        mPrinter.setPageXY(278, 50);
        mPrinter.printTaggedText("{reset}{s}MA{br}","ISO-8859-1");
        mPrinter.setPageXY(350, 50);
        mPrinter.printTaggedText("{reset}{s}"+billetFactory.getValue()+"{br}","ISO-8859-1");
        /* Segunda linha */
        mPrinter.setPageXY(278, 76);
        mPrinter.printTaggedText("{reset}{s}Total{br}","ISO-8859-1");
        mPrinter.setPageXY(350, 76);
        mPrinter.printTaggedText("{reset}{s}"+billetFactory.getValue()+"{br}","ISO-8859-1");
        mPrinter.setPageXY(263, 110);
        mPrinter.printTaggedText("{reset}Notifica��o N:{br}","ISO-8859-1");
        mPrinter.setPageXY(263, 144);
        mPrinter.printTaggedText("{reset}"+billetFactory.getLaunchNumber()+"{br}","ISO-8859-1");
        mPrinter.setPageXY(263, 183);
        mPrinter.printTaggedText("{reset}{s}OBSERVA��O: REFERENTE{br}","ISO-8859-1");
        mPrinter.setPageXY(263, 204);
        mPrinter.printTaggedText("{reset}{s}ESTACIONAMENTO{br}","ISO-8859-1");
        mPrinter.setPageXY(263, 225);
        mPrinter.printTaggedText("{reset}{s}ROTATIVO.{br}","ISO-8859-1");
        mPrinter.setPageXY(263, 255);
        mPrinter.printTaggedText("{reset}{s}N�O RECEBER AP�S{br}","ISO-8859-1");
        mPrinter.setPageXY(263, 276);
        mPrinter.printTaggedText("{reset}{s}O VENCIMENTO{br}","ISO-8859-1");
        mPrinter.setPageXY(263, 306);
        mPrinter.printTaggedText("{reset}{s}Emitido em:{br}","ISO-8859-1");
        mPrinter.setPageXY(263, 327);
        mPrinter.printTaggedText("{reset}{s}"+Base.retorneDataAtual()+"{br}","ISO-8859-1");
        mPrinter.setPageXY(263, 355);
        mPrinter.printTaggedText("{reset}Recibo do Sacado{br}","ISO-8859-1");
        
        //Tabela 1
        /* Seta a borda inicial do documento */
        Base.setBordaVertical("|", 21, 0, 15, mPrinter);
        /* Seta a borda de divis�o do documento */
        Base.setBordaVertical("|", 21, 250, 15, mPrinter);
        /* Seta a borda final do documento */
        Base.setBordaVertical("|", 16, 475, 15, mPrinter);
        /* Linha tracejada de corte */
        Base.setBordaVertical(".", 30, 495, 15, mPrinter);
        /* Seta a borda horizontal superior */
        Base.setBordaHorizontal("_", 31, 3, 0, mPrinter);
        /* Seta a borda horizontal inferior */
        Base.setBordaHorizontal("_", 16, 3, 360, mPrinter);
        /* Seta borda de divis�o das letras 'recibo do sacado' */
        Base.setBordaHorizontal("_", 15, 250, 330, mPrinter);
        mPrinter.printPage();
       
        mPrinter.reset();
        mPrinter.flush();
        mPrinter.selectStandardMode();
        mPrinter.feedPaper(10);
        mPrinter.selectPageMode();
        
        mPrinter.setPageRegion(0, 1380, 400, 2350, Printer.PAGE_TOP);
        mPrinter.setPageXY(11, 3);        
        mPrinter.printTaggedText("{reset}{h}"+" "+billetFactory.getLineType()+"{br}","ISO-8859-1"); 
        mPrinter.setPageXY(11, 63);
        mPrinter.printTaggedText("{reset}"+billetFactory.getTextoInformativoUsuario()+"{br}","ISO-8859-1");
        mPrinter.setPageXY(11, 96);            
        mPrinter.printTaggedText("{reset}{s}Data Emiss�o{br}","ISO-8859-1");
        mPrinter.setPageXY(11, 112);            
        mPrinter.printTaggedText("{reset}"+Base.retorneDataAtual()+"{br}","ISO-8859-1");
        mPrinter.setPageXY(337, 96);            
        mPrinter.printTaggedText("{reset}{s}Esp�cie{br}","ISO-8859-1");
        mPrinter.setPageXY(337, 112);            
        mPrinter.printTaggedText("{reset}Carn�{br}","ISO-8859-1");
        mPrinter.setPageXY(437, 96);            
        mPrinter.printTaggedText("{reset}{s}Aceite{br}","ISO-8859-1");
        mPrinter.setPageXY(437, 112);            
        mPrinter.printTaggedText("{reset}N{br}","ISO-8859-1");
        mPrinter.setPageXY(557, 96);            
        mPrinter.printTaggedText("{reset}{s}Valor Moeda{br}","ISO-8859-1");
        mPrinter.setPageXY(557, 112);            
        mPrinter.printTaggedText("","ISO-8859-1");
        // Decima primeira linha
        mPrinter.setPageXY(11, 150);            
        mPrinter.printTaggedText("{reset}{s}MA{br}","ISO-8859-1");
        mPrinter.setPageXY(91, 150);
        mPrinter.printTaggedText("{reset}{s}"+billetFactory.getValue()+"{br}","ISO-8859-1");
        mPrinter.setPageXY(11, 166);  
        mPrinter.printTaggedText("{reset}{s}Total{br}","ISO-8859-1");
        mPrinter.setPageXY(91, 166);            
        mPrinter.printTaggedText("{reset}{s}"+billetFactory.getValue()+"{br}","ISO-8859-1");   
        // Decima segunda linha
        mPrinter.setPageXY(11, 182);            
        mPrinter.printTaggedText("{reset}{s}Multa �rea Azul - Agente de Tr�nsito{br}","ISO-8859-1");
        mPrinter.setPageXY(11, 198);            
        mPrinter.printTaggedText("{reset}{s}OBSERVA��O: REFERENTE ESTACIONAMENTO ROTATIVO.{br}","ISO-8859-1");
        // Decima terceira linha
        mPrinter.setPageXY(11, 214);            
        mPrinter.printTaggedText("{reset}{s} - N�O RECEBER AP�S O VENCIMENTO {br}","ISO-8859-1");
        mPrinter.setPageXY(440, 214);//mudan�a            
        mPrinter.printTaggedText("{reset}{s}Emitido em: "+Base.retorneDataAtual()+"{br}","ISO-8859-1");
        //Linha da frente 
        mPrinter.setPageXY(441, 140);            
        mPrinter.printTaggedText("{reset}{s}Notifica��o N:{br}","ISO-8859-1");
        mPrinter.setPageXY(591, 140);            
        mPrinter.printTaggedText("{reset}{s}"+billetFactory.getLaunchNumber()+"{br}","ISO-8859-1");
        //Linha da frente 
        mPrinter.setPageXY(441, 158);            
        mPrinter.printTaggedText("{reset}{s}Placa:{br}","ISO-8859-1");
        mPrinter.setPageXY(591, 158);            
        //mPrinter.printTaggedText("{reset}"+PrinterActivity.this.infracao.getPlaca()+"{br}","ISO-8859-1");
        //Linha da frente 
        mPrinter.setPageXY(441, 182);            
        mPrinter.printTaggedText("{reset}{s}MUNIC�PIO:{br}","ISO-8859-1");
        mPrinter.setPageXY(591, 182);
        //mPrinter.printTaggedText("{reset}{s}"+PrinterActivity.this.infracao.getCidadeP()+"-"+PrinterActivity.this.infracao.getEstadoP()+"{br}","ISO-8859-1");
        // BarCode
        mPrinter.setPageXY(10, 250);
        mPrinter.setBarcode(Printer.ALIGN_LEFT, false,3, Printer.HRI_NONE, 161);//100
        //Impress�o 9600(porta)
        mPrinter.printBarcode(Printer.BARCODE_ITF, billetFactory.getBarcode());
        
        //Final form extrema direita
        mPrinter.setPageXY(732, 24);            
        mPrinter.printTaggedText("{reset}{s}Vencimento{br}","ISO-8859-1");
        mPrinter.setPageXY(732, 40);            
        mPrinter.printTaggedText("{reset}"+billetFactory.getMaturityDate()+"{br}","ISO-8859-1");
        //Terceira linha
        mPrinter.setPageXY(732, 180);            
        mPrinter.printTaggedText("{reset}{s}(=) Valor do Documento{br}","ISO-8859-1");
        mPrinter.setPageXY(732, 196);            
        mPrinter.printTaggedText("{reset}         "+billetFactory.getValue()+"{br}","ISO-8859-1");
        //Quinta linha
        mPrinter.setPageXY(732, 248);            
        mPrinter.printTaggedText("{reset}         {br}","ISO-8859-1");
      
        //String resource,int quantidade,int x,int distanciaInicial, Printer mPrinter
        /* Seta a borda esquerda inicial */
        Base.setBordaVertical("|", 10, 0, 5, mPrinter);
        /* Seta a borda direita divis�ria */
        Base.setBordaVertical("|", 9, 720, 20, mPrinter);//qnt-9
        /* Seta a borda direita final do documento */
        Base.setBordaVertical("|", 9, 960, 20, mPrinter);//960
       Base.setBordaVertical("|", 9, 960, 20, mPrinter);//960
        /*Borda horizontal mais alta da 2� parte da tabela */
        Base.setBordaHorizontal("_", 15, 723, 0, mPrinter);
        /* Seta a borda superior */
        Base.setBordaHorizontal("_", 47, 8, 35, mPrinter);
        /* Seta a borda inferior */
        Base.setBordaHorizontal("_", 63, 8, 210, mPrinter);//69
       
        
        mPrinter.printPage();
        mPrinter.flush();
        mPrinter.selectStandardMode();
        WAS_PRINTED = true;
        mPrinter.feedPaper(110);
        
	}
	
	
	
	
	
	
	/**
	 * Metodo que imprime o boleto
	 */
	private void printBoletoDate()
	{
		try {
            if (DEBUG) Log.d(LOG_TAG, "Print Page");
            mPrinter.reset();            
            mPrinter.selectPageMode();  
            //String text = "";
            

            
            StringBuilder sb = new StringBuilder();
            
            /**
    		 * Descer Itens (de cima para baixo) adicione x na fun��o .setPageXY
    		 * Mover itens da esquerda pra direita adicione y na fun��o .setPageXY
    		 * Distancia entre as linhas: 24
    		 * Quebra de linha: 51
    		 * A folha comporta 42 caracteres (incluindo espa�os) ->http://www.edsouza.net/contador-de-caracteres-online
    		 * Come�o do primeiro cabe�alho.
    		 */
            mPrinter.setPageRegion(0, 0, 400, 128, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 0);
            //Logo de concordia
            mBase.PrintLogo(getApplicationContext(), mPrinter);
            mPrinter.drawPageFrame(0, 0, 80, 80, Printer.FILL_WHITE, 1);
            /* Linha morta para evitar cortar o topo das letras da proxima linha
             * Cabe�alho cabe 34 caracteres
             */
            mPrinter.setPageXY(90, 2);
            mPrinter.printTaggedText("","ISO-8859-1");
            mPrinter.setPageXY(76, 8);
            mPrinter.printTaggedText("{reset}{s}{b}PREFEITURA MUNICIPAL DE CONC�RDIA{br}","ISO-8859-1");
            mPrinter.setPageXY(76, 30);
            mPrinter.printTaggedText("{reset}{s}Departatamento de Tr�nsito{br}","ISO-8859-1");
            mPrinter.setPageXY(76, 53);
            mPrinter.printTaggedText("{s}Rua Leonel Mosele, 62, Centro{br}","ISO-8859-1");
            mPrinter.setPageXY(76, 77);
            mPrinter.printTaggedText("{s}Conc�rdia SC (49)3441-2217{br}","ISO-8859-1");
            /*Quebra de linha, afasta o cabe�alho do texto subsequente
             * Novo cabe�alho para texto pr� formatado
             */
            
            mPrinter.printPage();
            mPrinter.flush();//m
            mPrinter.reset();
            mPrinter.selectStandardMode();
            
            sb.append("{reset}{br}{br}");
            sb.append("{reset}{b}{s}NOTIFICACAO/N� DO BOLETO "+mBase.getNumeroLanc()+"{br}");
            sb.append("{reset}{b}{s}Vencimento "+mBase.retorneDataVencimento()+"{br}");
            sb.append("{reset}{s}Regularizar at� 2 (dois) dias �teis, confo{br}");
            sb.append("{reset}{s}rme instru��es do boleto, onde ser� pago o{br}");
            sb.append("{reset}{s}valor de 5(UFIRS Municipal). Ap�s os dois {br}");
            sb.append("{reset}{s}dias �teis, se a presente notifica��o n�o {br}");
            sb.append("{reset}{s}for paga, ser� convertida em infra��o de t{br}");
            sb.append("{reset}{s}r�nsito, de acordo com o amparo legal, ART{br}");
            sb.append("{reset}{s}IGO, 181 - XVII do CTB.{br}{br}");
            sb.append("{reset}{s}Sr. usu�rio: na data {b}"+Base.retorneDataAtual()+"{/b} seu ve�cul{br}");
            sb.append("{reset}{s}o foi notificado as {b}"+Base.retorneHoraAtual()+"{/b}, de acordo{br}");
            sb.append("{reset}{s}com as penalidades previstas na legisla��o{br}");
            sb.append("{reset}{s}de tr�nsito em virtude de irregularidade c{br}");
            sb.append("{reset}{s}ometida no controle do estacionamento rota{br}");
            sb.append("{reset}{s}tivo.{br}{br}");
            //sb.append("{reset}[x] "+Util.obterTypeInfractionBy((int)PrinterActivity.this.infracao.getInfracao())+"{br}{br}{br}");
            sb.append("{reset}{s}IDENTIFICA��O, VE�CULO E LOCAL ESTACIONADO{br}");
            //sb.append("{reset}{s}Placa: {/s}"+PrinterActivity.this.infracao.getPlaca()+" | {s}"+PrinterActivity.this.infracao.getCidadeP()+" | "+PrinterActivity.this.infracao.getEstadoP()+"{br}");
            //sb.append("{reset}{s}Rua: "+PrinterActivity.this.infracao.getRua()+"  N�"+PrinterActivity.this.infracao.getNumero()+"{br}");
            //sb.append("{reset}{s}Marca/Modelo: "+PrinterActivity.this.infracao.getMarca()+"/"+PrinterActivity.this.infracao.getModelo()+"{br}{br}");
            sb.append("{reset}{center}N� AGENTE{br}");
            sb.append("{reset}{center}"+mBase.getNumeroAgente()+"{br}{br}");
            sb.append("{reset}{center}____________________________{br}");
            sb.append("{reset}{center}{s}Agente de Autoridade de Tr�nsito Municipal{br}{br}");
            mPrinter.printTaggedText(sb.toString(),"ISO-8859-1");
            mPrinter.printText("{reset}"+""+"{br}");
            mPrinter.flush();//m
            mPrinter.reset();
            mPrinter.selectPageMode();
            /***
             * Terceiro documento
             * Espa�amento entre linhas: 27
             * Espa�amento entre itens(cabe�alho + texto):16
			 * Espa�amento entre o cabe�alho e o form: 980
             */
            mPrinter.setPageRegion(0, 960, 400, 1460, Printer.PAGE_TOP);
                      
            //Primeira linha
            mPrinter.setPageXY(11, 24);            
            mPrinter.printTaggedText("{reset}{s}Vencimento{br}","ISO-8859-1");
            mPrinter.setPageXY(11, 40);
            mPrinter.printTaggedText("{reset}"+mBase.retorneDataVencimento()+"{br}","ISO-8859-1");     
            //Segunda linha
            mPrinter.setPageXY(11, 67);            
            mPrinter.printTaggedText("{reset}{s}Ag�ncia/C�d. Cedente{br}","ISO-8859-1");
            mPrinter.setPageXY(11, 83);
            mPrinter.printTaggedText("{reset}"+mBase.getAgenciaCod()+"{br}","ISO-8859-1");
            //Terceira linha
            mPrinter.setPageXY(11, 110);            
            mPrinter.printTaggedText("{reset}{s}(=) Valor Documento{br}","ISO-8859-1");
            mPrinter.setPageXY(11, 126);
            mPrinter.printTaggedText("{reset}{left}        "+mBase.getValor()+"{br}","ISO-8859-1");
            //Quinta linha
            mPrinter.setPageXY(11, 166);            
            mPrinter.printTaggedText("{reset}{s}Nosso N�mero{br}","ISO-8859-1");
            mPrinter.setPageXY(11, 182);
            mPrinter.printTaggedText("{reset}"+mBase.getNossoNumero()+"{br}","ISO-8859-1");
            //Sexta linha
            mPrinter.setPageXY(11, 209);            
            mPrinter.printTaggedText("{reset}{s}N�mero Documento{br}","ISO-8859-1");
            mPrinter.setPageXY(11, 225);
            mPrinter.printTaggedText("{reset}"+mBase.getNumeroDoc()+"{br}","ISO-8859-1");
            //Setima linha
            mPrinter.setPageXY(11, 252);            
            mPrinter.printTaggedText("{reset}{s}Nro. Lan�amento{br}","ISO-8859-1");
            mPrinter.setPageXY(11, 268);
            mPrinter.printTaggedText("{reset}"+mBase.getNumeroLanc()+"{br}","ISO-8859-1");
            //Oitava linha
            mPrinter.setPageXY(11, 297);            
            mPrinter.printTaggedText("{reset}{s}Placa:{br}","ISO-8859-1");
            mPrinter.setPageXY(11, 316);
            //mPrinter.printTaggedText("{reset}"+PrinterActivity.this.infracao.getPlaca()+"{br}","ISO-8859-1");
            mPrinter.setPageXY(11, 342);
            mPrinter.printTaggedText("{reset}{s}Munic�pio:{br}","ISO-8859-1");
            mPrinter.setPageXY(11, 362);
            //mPrinter.printTaggedText("{reset}{s}"+PrinterActivity.this.infracao.getCidadeP()+" "+PrinterActivity.this.infracao.getEstadoP()+"{br}","ISO-8859-1");
           
            /* Parte do meio */
            mPrinter.setPageXY(263, 24);
            mPrinter.printTaggedText("{reset}{s}Outras Informa��es{br}","ISO-8859-1");
            /* Primeira linha */
            mPrinter.setPageXY(278, 50);
            mPrinter.printTaggedText("{reset}{s}MA{br}","ISO-8859-1");
            mPrinter.setPageXY(350, 50);
            mPrinter.printTaggedText("{reset}{s}"+mBase.getValor()+"{br}","ISO-8859-1");
            /* Segunda linha */
            mPrinter.setPageXY(278, 76);
            mPrinter.printTaggedText("{reset}{s}Total{br}","ISO-8859-1");
            mPrinter.setPageXY(350, 76);
            mPrinter.printTaggedText("{reset}{s}"+mBase.getValor()+"{br}","ISO-8859-1");
            mPrinter.setPageXY(263, 110);
            mPrinter.printTaggedText("{reset}Notifica��o N:{br}","ISO-8859-1");
            mPrinter.setPageXY(263, 144);
            mPrinter.printTaggedText("{reset}"+mBase.getNumeroLanc()+"{br}","ISO-8859-1");
            mPrinter.setPageXY(263, 183);
            mPrinter.printTaggedText("{reset}{s}OBSERVA��O: REFERENTE{br}","ISO-8859-1");
            mPrinter.setPageXY(263, 204);
            mPrinter.printTaggedText("{reset}{s}ESTACIONAMENTO{br}","ISO-8859-1");
            mPrinter.setPageXY(263, 225);
            mPrinter.printTaggedText("{reset}{s}ROTATIVO.{br}","ISO-8859-1");
            mPrinter.setPageXY(263, 255);
            mPrinter.printTaggedText("{reset}{s}N�O RECEBER AP�S{br}","ISO-8859-1");
            mPrinter.setPageXY(263, 276);
            mPrinter.printTaggedText("{reset}{s}O VENCIMENTO{br}","ISO-8859-1");
            mPrinter.setPageXY(263, 306);
            mPrinter.printTaggedText("{reset}{s}Emitido em:{br}","ISO-8859-1");
            mPrinter.setPageXY(263, 327);
            mPrinter.printTaggedText("{reset}{s}"+Base.retorneDataAtual()+"{br}","ISO-8859-1");
            mPrinter.setPageXY(263, 355);
            mPrinter.printTaggedText("{reset}Recibo do Sacado{br}","ISO-8859-1");
            
            //Tabela 1
            /* Seta a borda inicial do documento */
            Base.setBordaVertical("|", 21, 0, 15, mPrinter);
            /* Seta a borda de divis�o do documento */
            Base.setBordaVertical("|", 21, 250, 15, mPrinter);
            /* Seta a borda final do documento */
            Base.setBordaVertical("|", 16, 475, 15, mPrinter);
            /* Linha tracejada de corte */
            Base.setBordaVertical(".", 30, 495, 15, mPrinter);
            /* Seta a borda horizontal superior */
            Base.setBordaHorizontal("_", 31, 3, 0, mPrinter);
            /* Seta a borda horizontal inferior */
            Base.setBordaHorizontal("_", 16, 3, 360, mPrinter);
            /* Seta borda de divis�o das letras 'recibo do sacado' */
            Base.setBordaHorizontal("_", 15, 250, 330, mPrinter);
            mPrinter.printPage();
           
            mPrinter.reset();
            mPrinter.flush();
            mPrinter.selectStandardMode();
            mPrinter.feedPaper(10);
            mPrinter.selectPageMode();
            
            mPrinter.setPageRegion(0, 1380, 400, 2350, Printer.PAGE_TOP);
            mPrinter.setPageXY(11, 3);        
            mPrinter.printTaggedText("{reset}{h}"+mBase.getNumeroGeral()+"{br}","ISO-8859-1"); 
            mPrinter.setPageXY(11, 63);
            mPrinter.printTaggedText("{reset}PAG�VEL PREFERENCIALEMENTE NAS LOT�RICAS DA CAIXA.{br}","ISO-8859-1");
            mPrinter.setPageXY(11, 96);            
            mPrinter.printTaggedText("{reset}{s}Data Emiss�o{br}","ISO-8859-1");
            mPrinter.setPageXY(11, 112);            
            mPrinter.printTaggedText("{reset}"+Base.retorneDataAtual()+"{br}","ISO-8859-1");
            mPrinter.setPageXY(167, 96);            
            mPrinter.printTaggedText("{reset}{s}N�mero Documento{br}","ISO-8859-1");
            mPrinter.setPageXY(167, 112);            
            mPrinter.printTaggedText("{reset}"+mBase.getNumeroDoc()+"{br}","ISO-8859-1");
            mPrinter.setPageXY(337, 96);            
            mPrinter.printTaggedText("{reset}{s}Esp�cie{br}","ISO-8859-1");
            mPrinter.setPageXY(337, 112);            
            mPrinter.printTaggedText("{reset}Carn�{br}","ISO-8859-1");
            mPrinter.setPageXY(437, 96);            
            mPrinter.printTaggedText("{reset}{s}Aceite{br}","ISO-8859-1");
            mPrinter.setPageXY(437, 112);            
            mPrinter.printTaggedText("{reset}N{br}","ISO-8859-1");
            mPrinter.setPageXY(557, 96);            
            mPrinter.printTaggedText("{reset}{s}Valor Moeda{br}","ISO-8859-1");
            mPrinter.setPageXY(557, 112);            
            mPrinter.printTaggedText("","ISO-8859-1");
            // Decima primeira linha
            mPrinter.setPageXY(11, 150);            
            mPrinter.printTaggedText("{reset}{s}MA{br}","ISO-8859-1");
            mPrinter.setPageXY(91, 150);
            mPrinter.printTaggedText("{reset}{s}"+mBase.getValor()+"{br}","ISO-8859-1");
            mPrinter.setPageXY(11, 166);  
            mPrinter.printTaggedText("{reset}{s}Total{br}","ISO-8859-1");
            mPrinter.setPageXY(91, 166);            
            mPrinter.printTaggedText("{reset}{s}"+mBase.getValor()+"{br}","ISO-8859-1");   
            // Decima segunda linha
            mPrinter.setPageXY(11, 182);            
            mPrinter.printTaggedText("{reset}{s}Multa �rea Azul - Agente de Tr�nsito{br}","ISO-8859-1");
            mPrinter.setPageXY(11, 198);            
            mPrinter.printTaggedText("{reset}{s}OBSERVA��O: REFERENTE ESTACIONAMENTO ROTATIVO.{br}","ISO-8859-1");
            // Decima terceira linha
            mPrinter.setPageXY(11, 214);            
            mPrinter.printTaggedText("{reset}{s} - N�O RECEBER AP�S O VENCIMENTO {br}","ISO-8859-1");
            mPrinter.setPageXY(440, 214);//mudan�a            
            mPrinter.printTaggedText("{reset}{s}Emitido em: "+Base.retorneDataAtual()+"{br}","ISO-8859-1");
            //Linha da frente 
            mPrinter.setPageXY(441, 140);            
            mPrinter.printTaggedText("{reset}{s}Notifica��o N:{br}","ISO-8859-1");
            mPrinter.setPageXY(591, 140);            
            mPrinter.printTaggedText("{reset}{s}"+mBase.getNumeroLanc()+"{br}","ISO-8859-1");
            //Linha da frente 
            mPrinter.setPageXY(441, 158);            
            mPrinter.printTaggedText("{reset}{s}Placa:{br}","ISO-8859-1");
            mPrinter.setPageXY(591, 158);            
            //mPrinter.printTaggedText("{reset}"+PrinterActivity.this.infracao.getPlaca()+"{br}","ISO-8859-1");
            //Linha da frente 
            mPrinter.setPageXY(441, 182);            
            mPrinter.printTaggedText("{reset}{s}MUNIC�PIO:{br}","ISO-8859-1");
            mPrinter.setPageXY(591, 182);            
            //mPrinter.printTaggedText("{reset}{s}"+PrinterActivity.this.infracao.getCidadeP()+"-"+PrinterActivity.this.infracao.getEstadoP()+"{br}","ISO-8859-1");
            // BarCode
            mPrinter.setPageXY(10, 250);
            mPrinter.setBarcode(Printer.ALIGN_LEFT, false,3, Printer.HRI_NONE, 161);//100
            //Impress�o 9600(porta)
            mPrinter.printBarcode(Printer.BARCODE_ITF, mBase.getNumberBarcode());
            
            //Final form extrema direita
            mPrinter.setPageXY(732, 24);            
            mPrinter.printTaggedText("{reset}{s}Vencimento{br}","ISO-8859-1");
            mPrinter.setPageXY(732, 40);            
            mPrinter.printTaggedText("{reset}"+mBase.retorneDataVencimento()+"{br}","ISO-8859-1");
            //Terceira linha
            mPrinter.setPageXY(732, 76);            
            mPrinter.printTaggedText("{reset}{s}Ag�ncia/C�d. Cedente{br}","ISO-8859-1");
            mPrinter.setPageXY(732, 92);            
            mPrinter.printTaggedText("{reset}"+mBase.getAgenciaCod()+"{br}","ISO-8859-1");
            //Segunda linha
            mPrinter.setPageXY(732, 128);            
            mPrinter.printTaggedText("{reset}{s}Nosso N�mero{br}","ISO-8859-1");
            mPrinter.setPageXY(732, 144);            
            mPrinter.printTaggedText("{reset}{s}"+mBase.getNossoNumero()+"{br}","ISO-8859-1");
            //Quarta linha
            mPrinter.setPageXY(732, 180);            
            mPrinter.printTaggedText("{reset}{s}(=) Valor do Documento{br}","ISO-8859-1");
            mPrinter.setPageXY(732, 196);            
            mPrinter.printTaggedText("{reset}         "+mBase.getValor()+"{br}","ISO-8859-1");
            mPrinter.setPageXY(732, 248);            
            mPrinter.printTaggedText("{reset}         {br}","ISO-8859-1");
          
            //String resource,int quantidade,int x,int distanciaInicial, Printer mPrinter
            /* Seta a borda esquerda inicial */
            Base.setBordaVertical("|", 10, 0, 5, mPrinter);
            /* Seta a borda direita divis�ria */
            Base.setBordaVertical("|", 9, 720, 20, mPrinter);//qnt-9
            /* Seta a borda direita final do documento */
            Base.setBordaVertical("|", 9, 960, 20, mPrinter);//960
           Base.setBordaVertical("|", 9, 960, 20, mPrinter);//960
            /*Borda horizontal mais alta da 2� parte da tabela */
            Base.setBordaHorizontal("_", 15, 723, 0, mPrinter);
            /* Seta a borda superior */
            Base.setBordaHorizontal("_", 47, 8, 35, mPrinter);
            /* Seta a borda inferior */
            Base.setBordaHorizontal("_", 63, 8, 210, mPrinter);//69
           
            
            mPrinter.printPage();
            mPrinter.flush();
            mPrinter.selectStandardMode();
            WAS_PRINTED = true;
            mPrinter.feedPaper(110);
            
            
        } catch (IOException e) {
        	e.printStackTrace();
            error(getString(R.string.msg_failed_to_print_page) + ". " + e.getMessage(), mRestart);            
        }
	}


}

