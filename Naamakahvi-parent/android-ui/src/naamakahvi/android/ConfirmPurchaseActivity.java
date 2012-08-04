package naamakahvi.android;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import naamakahvi.android.R;
import naamakahvi.android.utils.Basket;
import naamakahvi.android.utils.Config;
import naamakahvi.android.utils.ExtraNames;
import naamakahvi.naamakahviclient.Client;
import naamakahvi.naamakahviclient.ClientException;
import naamakahvi.naamakahviclient.IProduct;
import naamakahvi.naamakahviclient.IStation;
import naamakahvi.naamakahviclient.IUser;

public class ConfirmPurchaseActivity extends Activity {

	final short COUNTDOWN_LENGTH = 10;
	private CountDownTimer cd;
	private Intent intent;
	private String username;
	private IUser buyer;
	private Handler handler;
	public static final String TAG = "ConfirmPurchaseActivity";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.confirm_purchase);
		intent = getIntent();
		setCountdown();
        ListView possibleUsersListView = (ListView) findViewById(R.id.possibleUsers);
        String[] listOfPossibleUsers = intent.getStringArrayExtra(ExtraNames.USERS);
        setListView(possibleUsersListView, listOfPossibleUsers);
        username = listOfPossibleUsers[0];
        handler = new Handler();
        startGetIUserThread();
        setSaldos(listOfPossibleUsers[0]);
        setRecognizedText(listOfPossibleUsers[0]);
	}

	private void startGetIUserThread() {
		new Thread(new Runnable() {
			public void run() {
				try {
					List<IStation> s = Client.listStations(Config.SERVER_URL,Config.SERVER_PORT);
					Client c = new Client(Config.SERVER_URL,Config.SERVER_PORT, s.get(0));
					IUser user = c.authenticateText(username);
					buyer = user;
				}
				catch (final ClientException ex) {
					Log.d(TAG, ex.getMessage());
					ex.printStackTrace();
				}
			}			
		}).start();
	}
	
	private void setListView(ListView listView, String[] list) {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
            	android.R.layout.simple_list_item_1, android.R.id.text1, list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view,
        		int position, long id) {
        		String alternativeUser = (String) parent.getAdapter().getItem(position);
        		username = alternativeUser;
        		startGetIUserThread();
        		setSaldos(alternativeUser);
        		setRecognizedText(alternativeUser);
        		cd.cancel();
        		cd.start();
        	}
        }); 
	}
	
	private void setRecognizedText(String username) {
		TextView recognized = (TextView) findViewById(R.id.cp_nametext);
		String newRecognizedText = "You were recognized as: " + username;
		recognized.setText(newRecognizedText);
	}
	
	private void setSaldos(String username) {
		Basket producstThatCustomerIsBuying = intent.getParcelableExtra(ExtraNames.PRODUCTS);
		Map<IProduct, Integer> productsToBeBought = producstThatCustomerIsBuying.getItems();
		int changeInEspresso = 0;
		int changeInCoffee = 0;
		
		// TODO: alla olevaa muutetaan, kun saadaan productiin metodit, jotka kertovat hinnan!
		Iterator productsAndAmounts = productsToBeBought.entrySet().iterator();
	    while (productsAndAmounts.hasNext()) {
	        Map.Entry productAndAmountPair = (Map.Entry)productsAndAmounts.next();
	        IProduct product = (IProduct) productAndAmountPair.getKey();
	        int amount = (Integer) productAndAmountPair.getValue();
	        if (product.getName().equals("Kahvi"))
	        	changeInCoffee -= (amount*product.getPrice());
	        else
	        	changeInEspresso -= (amount*product.getPrice());
	    }
		
		TextView saldoEspresso = (TextView) findViewById(R.id.saldoEspresso);
		TextView saldoCoffee = (TextView) findViewById(R.id.saldoCoffee);
		
		//TODO: get saldos from client, currently testSaldos used instead.
		int testSaldoCof = -2;
		int testSaldoEsp = 4;
		String newTextForSaldoEspresso = "Your espressosaldo is " + testSaldoEsp + " + " + changeInEspresso;
		String newTextForSaldoCoffee = "Your coffeesaldo is " + testSaldoCof + " + " + changeInCoffee;
		saldoCoffee.setText(newTextForSaldoCoffee);
		saldoEspresso.setText(newTextForSaldoEspresso);
		
		if ((testSaldoCof + changeInCoffee) >= 0)
			saldoCoffee.setTextColor(Color.GREEN);
		else
			saldoCoffee.setTextColor(Color.RED);
		
		if ((testSaldoEsp + changeInEspresso) >= 0)
			saldoEspresso.setTextColor(Color.GREEN);
		else
			saldoEspresso.setTextColor(Color.RED);
	}
	
	private void setCountdown() {
		final TextView countdown = (TextView) findViewById(R.id.cp_rec_countdown);
		countdown.setText(getString(R.string.countdown_prefix) + " "
				+ COUNTDOWN_LENGTH + getString(R.string.countdown_suffix));
		
		cd = new CountDownTimer(1000 * COUNTDOWN_LENGTH, 1000) {
			
			@Override
			public void onTick(long timeLeft) {
				countdown.setText(getString(R.string.countdown_prefix) + " "
						+ timeLeft / 1000
						+ getString(R.string.countdown_suffix));

			}
			
			@Override
			public void onFinish() {
				buyProducts();
				setResult(RESULT_OK);
				finish();
			}
		};
		cd.start();
	}
	
	public void onCPOkClick(View v) {
		buyProducts();
		cd.cancel();
		setResult(RESULT_OK);
		finish();
	}
	
	private void buyProducts() {
		Basket b = intent.getParcelableExtra(ExtraNames.PRODUCTS);
		Map<IProduct, Integer> itemsBought = b.getItems();
		Iterator productsAndAmounts = itemsBought.entrySet().iterator();
		startBuyingThread(productsAndAmounts);
	}
	
	private void startBuyingThread(Iterator its) {
		final Iterator productsAndAmounts = its;
		new Thread(new Runnable() {
			public void run() {
				try {
					List<IStation> s = Client.listStations(Config.SERVER_URL,Config.SERVER_PORT);
					Client c = new Client(Config.SERVER_URL,Config.SERVER_PORT, s.get(0));
					while (productsAndAmounts.hasNext()) {
				        Map.Entry productAndAmountPair = (Map.Entry)productsAndAmounts.next();
				        IProduct product = (IProduct) productAndAmountPair.getKey();
				        int amount = (Integer) productAndAmountPair.getValue();
				        c.buyProduct(buyer, product, amount);
				    }
				}
				catch (final ClientException ex) {
					Log.d(TAG, ex.getMessage());
					ex.printStackTrace();
				}
			}			
		}).start();
	}
	
	
	public void onCPCancelClick(View v) {
		setResult(RESULT_CANCELED);
		finish();
	}
}
