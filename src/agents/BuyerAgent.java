package agents;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

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
		
		addBehaviour(new BuyerBehaviour());
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
	
	/*
	 * The agent will operate as a Finite State Machine (FSM).
	 * Each state is represented by a internal Behaviour.
	 */
	private class BuyerBehaviour extends FSMBehaviour {
		private static final long serialVersionUID = 4617454455142095086L;
		
		private MessageTemplate messageTemplate;
		
		public BuyerBehaviour() {
			messageTemplate = new MessageTemplate(null);
			registerFirstState(new WaitAuctionBehaviour(), "waiting for auction");
			
			/* Empty state only to simulate transition. 
			 * Will be removed when proper end states are implemented.
			 */
			registerLastState(new OneShotBehaviour() {
				private static final long serialVersionUID = 3438209180132381071L;

				@Override
				public void action() {
				}
			}, "ending auction");
			
			registerDefaultTransition("waiting for auction", "ending auction");
		}
		
		/*
		 * Waits for an inform from an auctioneer that the auction is about to start.
		 */
		private class WaitAuctionBehaviour extends Behaviour {
			private static final long serialVersionUID = -6347094125830185348L;
			
			private boolean stopWaiting = false;

			@Override
			public void action() {
				messageTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
				ACLMessage inform = myAgent.receive(messageTemplate);
				
				if (inform != null) {
					System.out.println("Buyer [" + getAID().getLocalName() + "] was informed.");
					stopWaiting = true;
				} else {
					block();
				}
			}

			@Override
			public boolean done() {
				return stopWaiting;
			}
		}
	}
}
