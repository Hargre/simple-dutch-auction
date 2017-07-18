package agents;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class AuctioneerAgent extends Agent {
	private static final long serialVersionUID = 667090917660533415L;
	
	protected void setup() {
		DFAgentDescription auctioneerDescription = new DFAgentDescription();
		auctioneerDescription.setName(getAID());
		
		ServiceDescription auctioneerService = new ServiceDescription();
		auctioneerService.setName("auctioneer");
		auctioneerService.setType("flower-auctioneer");
		auctioneerDescription.addServices(auctioneerService);
		
		try {
			DFService.register(this, auctioneerDescription);
		} catch (FIPAException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	protected void takeDown() {
		try {
			System.out.println("Terminating auctioneer [" + getAID().getLocalName() + "]...");
			DFService.deregister(this);
		} catch (FIPAException ex) {
			ex.printStackTrace();
		}
	}
	
	
}
