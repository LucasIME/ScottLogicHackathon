package trading;

import game.TradingManager;

import tradingstrategy.BaseTradingStrategy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import dataobjects.DailyInput;
import dataobjects.DailyTrades;
import exceptions.InsufficientFundsException;
import exceptions.InsufficientSharesException;



public class TradingStrategy extends BaseTradingStrategy {
	
	List<List<DailyInput>> history = new ArrayList<List<DailyInput>>();
	
	List<List<Double>> trends = new ArrayList<List<Double>>();

	public TradingStrategy(TradingManager tradingManager) {
		super.tradingManager = tradingManager;
		history.add(new LinkedList<DailyInput>());
		history.add(new LinkedList<DailyInput>());
		history.add(new LinkedList<DailyInput>());
		
		trends.add(new ArrayList<Double>());
		trends.add(new ArrayList<Double>());
		trends.add(new ArrayList<Double>());
		for(List<Double> a: trends){
			a.add(0.0);
			a.add(0.0);
			a.add(0.0);
			a.add(0.0);
		}
	}
	
	

	@Override
	public void makeDailyTrade(DailyTrades trades) throws InsufficientFundsException, InsufficientSharesException {
		//use the trading manager to make trades based on input
		int company = 0;
		int day = trades.getDay();
		System.out.println("Day "+ trades.getDay());
		for (DailyInput trade : trades.getTrades()) {
			history.get(company).add(trade);
			
			//One day history
			trends.get(company).set(0, getGraphTrend(history.get(company), day-1));
			if(day>=7){
				//Week history
				trends.get(company).set(1, getGraphTrend(history.get(company), day-7));
				if(day>=30){
					//Month history
					trends.get(company).set(2, getGraphTrend(history.get(company), day-30));
					if(day>30){
						//Complete history
						trends.get(company).set(3, getGraphTrend(history.get(company), 0));
					}
				}
			}
			company++;
		}
		
		int scaling_Factor = 40;
		int short_term_length = (int) day/5;
		int long_term_length = 3*short_term_length;

		company = 0;
		for (DailyInput input : trades.getTrades()) {
			
			double short_term_sum = EmovingAverage(history.get(company), short_term_length);
			double long_term_sum = EmovingAverage(history.get(company), long_term_length);
			System.out.println(short_term_sum);
			System.out.println(long_term_sum);
			
			if(short_term_sum>long_term_sum){
				//if still have enough money
				if(tradingManager.getAvailableFunds()!=0){
					int num_shares_to_buy = (int) (scaling_Factor*short_term_sum/long_term_sum);
					System.out.println("Buying "+num_shares_to_buy+" of "+company);
					if((input.getClose()*num_shares_to_buy)>tradingManager.getAvailableFunds()){
						tradingManager.buyMaxNumberOfShares(input);
					}else{
						tradingManager.buyNumberOfShares(input, num_shares_to_buy);
					}
				}
			}else{
				//if still have shares in company
				if(tradingManager.getSharesOwned(input.getCompany())!=0){	
					int num_shares_to_sell = (int) (scaling_Factor*long_term_sum/short_term_sum);
					System.out.println("Selling "+num_shares_to_sell+" of "+company);
					if(tradingManager.getSharesOwned(input.getCompany())<num_shares_to_sell){
						tradingManager.sellAllShares(input);
					}else{
						tradingManager.sellNumberOfShares(input, num_shares_to_sell);
					}
				}
			}
			company++;
		}
	}
	
	
	double movingAverage(List<DailyInput> points, int days){

		int s = points.size();
		int i = s-days;
		double sum = 0;
		while(i<s){
			sum += points.get(i).getClose();
			i++;
		}

		return sum / days;
		
	}
	
	double EmovingAverage(List<DailyInput> points, int days){
		int normaliser = 0;
		for(int i = 0; i<days; i++){
			normaliser += Math.exp(-(i));
		}
		
		int s = points.size();
		int i = s-days;
		double sum = 0;
		while(i<s){
			sum += Math.exp(-(i+days-s))*points.get(i).getClose()/normaliser;
			i++;
		}

		return sum / days;
		
	}
	
	double getGraphTrend(List<DailyInput> points, int day){
		int MAXN = 4*(points.size()-day);
	    int index = 0;
	    int x = 0;
	    double[] xs = new double[MAXN];
	    double[] ys = new double[MAXN];
	
	    // first pass: read in data, compute xbar and ybar
	    double sumx = 0.0, sumy = 0.0;
	    
	    while(day<points.size()){
	        xs[index] = x;
	        ys[index] = points.get(day).getOpen();
	        sumx  += xs[index];
	        sumy  += ys[index];
	        index++;
	        x++;
	        
	        //these 2 middles ones are plotted at the same x point so only increment x after
	        xs[index] = x;
	        ys[index] = points.get(day).getHigh();
	        sumx  += xs[index];
	        sumy  += ys[index];
	        index++;
	        
	        xs[index] = x;
	        ys[index] = points.get(day).getLow();
	        sumx  += xs[index];
	        sumy  += ys[index];
	        index++;
	        x++;
	        
	        xs[index] = x;
	        ys[index] = points.get(day).getClose();
	        sumx  += xs[index];
	        sumy  += ys[index];
	        index++;
	        x++;
	        
	        day++;
	    }
	    
	    double xbar = sumx / index;
	    double ybar = sumy / index;
	
	    // second pass: compute summary statistics
	    double xxbar = 0.0, xybar = 0.0;
	    for (int i = 0; i < index; i++) {
	        xxbar += (xs[i] - xbar) * (xs[i] - xbar);
	        xybar += (xs[i] - xbar) * (ys[i] - ybar);
	    }
	    //slope
	    double m = xybar / xxbar;
	   
	    return m;
	}
	
}

