package agents;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class BuyerAgent extends Agent {
	private static final long serialVersionUID = 76034717775332120L;
	
	protected void setup() {
		DFAgentDescription buyerDescription = new DFAgentDescription();
		buyerDescription.setName(getAID());
		
		ServiceDescription buyerService = new ServiceDescription();
		buyerService.setName("buyer");
		buyerService.setType("flower-buyer");
		buyerDescription.addServices(buyerService);
		
		try {
			DFService.register(this, buyerDescription);
		} catch (FIPAException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	protected void takeDown() {
		try {
			System.out.println("Terminating buyer [" + getAID().getLocalName() + "]...");
			DFService.deregister(this);
		} catch (FIPAException ex) {
			ex.printStackTrace();
		}
	}
}
