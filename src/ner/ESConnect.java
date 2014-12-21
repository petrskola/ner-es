/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ner;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

/**
 *
 * @author Petr
 */
public class ESConnect {
	private Client client;
		
	public ESConnect (){
			Client client = new TransportClient().addTransportAddress(new InetSocketTransportAddress("localhost",9300));
			this.client = client;
	}
	
	public void endSession(){
		this.client.close();
	}
	
	public void loadNerUpdate(NameTag nt){
		SearchResponse scrollResp = this.client.prepareSearch("banky")
						.setSearchType(SearchType.SCAN)
						.setScroll(new TimeValue(30000))
						.setQuery(QueryBuilders.matchAllQuery())
						.setIndices("banky")
						.addFields("message")
						.setSize(100).execute().actionGet(); //100 hits per shard will be returned for each scroll
		//Scroll until no hits are returned
		long i = 0;
		while (true) {
			i++;
			try{
				for (SearchHit hit : scrollResp.getHits()) {
					if(hit.fields().containsKey("message")){
						this.client.prepareUpdate("banky", hit.getType(), hit.getId()).setDoc("ner", nt.doNer(hit.field("message").getValue().toString())).execute().actionGet();
					}
				}

				scrollResp = this.client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(30000)).execute().actionGet();
				//Break condition: No hits are returned
				if (scrollResp.getHits().getHits().length == 0) {
					break;
				}
			}catch(Exception ex){
				Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
				break;
			}
			if (i%1000==0) System.out.println(i);
		}
	}
}

