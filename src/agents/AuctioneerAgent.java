package agents;

import java.util.ArrayList;
import java.util.List;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
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
		
		addBehaviour(new AuctioneerBehaviour());
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
	
	/*
	 * The agent will operate as a Finite State Machine (FSM).
	 * Each state is represented by a internal Behaviour.
	 */
	private class AuctioneerBehaviour extends FSMBehaviour {
		private static final long serialVersionUID = -7791885794766458598L;
		private List<AID> buyers;
		
		public AuctioneerBehaviour() {
			buyers = new ArrayList<>();
			
			registerFirstState(new SearchBuyersBehaviour(), "searching for buyers");
			
			/* Empty state only to simulate transition. 
			 * Will be removed when proper end states are implemented.
			 */
			registerLastState(new OneShotBehaviour() {
				private static final long serialVersionUID = 3438209180132381071L;

				@Override
				public void action() {
				}
			}, "ending auction");
			
			registerDefaultTransition("searching for buyers", "ending auction");
		}
		/*
		 * Looks for existing buyers so the auction can start.
		 */
		private class SearchBuyersBehaviour extends OneShotBehaviour {
			private static final long serialVersionUID = -5348961045040999273L;

			@Override
			public void action() {
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription serviceTemplate = new ServiceDescription();
				serviceTemplate.setType("flower-buyer");
				template.addServices(serviceTemplate);
				
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					if (result.length == 0) {
						System.out.println("No buyers found...");
					} else {
						System.out.println("Buyers found:");
						for (int i = 0; i < result.length; i++) {
							buyers.add(result[i].getName());
							System.out.println(buyers.get(i).getName());
						}
					}
				} catch (FIPAException ex) {
					ex.printStackTrace();
				}
			}		
		}
	}
}
