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
	
	private double priceToBuy;
	
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
		
		getPriceToBuy();
		
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
	
	private void getPriceToBuy() {
		Object args[] = getArguments();
		
		if (args != null && args.length >= 1) {
			try {
				priceToBuy = Double.parseDouble(args[0].toString());
			} catch (NumberFormatException ex) {
				System.out.println("Please type the interested price for the auction, in decimal form.");
				doDelete();
			}
		} else {
			System.out.println("Buying price not determined, terminating agent...");
			doDelete();
		}
	}
	
	/*
	 * The agent will operate as a Finite State Machine (FSM).
	 * Each state is represented by a internal Behaviour.
	 */
	private class BuyerBehaviour extends FSMBehaviour {
		private static final long serialVersionUID = 4617454455142095086L;
		
		// Constants for transition states 
		private final int noBuyers = 0;
		private final int sendProposal = 1;
		private final int highPrice = 2;
		
		public BuyerBehaviour() {
			registerFirstState(new WaitAuctionBehaviour(), "waiting for auction");
			registerState(new ReceiveCfpBehaviour(), "waiting for cfp");
			
			/* Empty state only to simulate transition. 
			 * Will be removed when proper end states are implemented.
			 */
			registerLastState(new OneShotBehaviour() {
				private static final long serialVersionUID = 3438209180132381071L;

				@Override
				public void action() {
				}
			}, "ending auction");
			
			registerDefaultTransition("waiting for auction", "waiting for cfp");
			registerTransition("waiting for cfp", "ending auction", noBuyers);
			registerTransition("waiting for cfp", "ending auction", sendProposal); // TODO: receive proposal response
			registerTransition("waiting for cfp", "waiting for cfp", highPrice);
		}
		
		/*
		 * Waits for an inform from an auctioneer that the auction is about to start.
		 */
		private class WaitAuctionBehaviour extends Behaviour {
			private static final long serialVersionUID = -6347094125830185348L;
			
			private boolean stopWaiting = false;

			@Override
			public void action() {
				MessageTemplate messageTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
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
		
		private class ReceiveCfpBehaviour extends Behaviour {
			private static final long serialVersionUID = 289640441682152767L;
			
			private boolean hasReceivedMessage = false;
			private int transitionStatus;

			@Override
			public void action() {
				/*
				 * There are two possible types of message to be received:
				 * A CFP, indicating the auctioneer is still trying to sell;
				 * An inform, indicating that the auction has ended.
				 */
				MessageTemplate cfpTemplate = MessageTemplate.MatchPerformative(ACLMessage.CFP);
				MessageTemplate informTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
				MessageTemplate messageTemplate = MessageTemplate.or(cfpTemplate, informTemplate);
				ACLMessage receivedMessage = myAgent.receive(messageTemplate);
				
				if (receivedMessage != null) {
					hasReceivedMessage = true;
					
					if (receivedMessage.getPerformative() == ACLMessage.CFP) {
						double price = Double.parseDouble(receivedMessage.getContent());
						System.out.println("Agent [" + getAID().getLocalName() + "] received "
								+ "CFP with price " + Double.toString(price));
						
						// Value in range; send proposal
						if (price <= priceToBuy) {
							ACLMessage proposal = new ACLMessage(ACLMessage.PROPOSE);
							proposal.addReceiver(receivedMessage.getSender());
							proposal.setProtocol(receivedMessage.getProtocol());
							myAgent.send(proposal);
							
							transitionStatus = sendProposal;
						} else {
							// Price too high; refuse and wait for new CFP
							ACLMessage refusal = new ACLMessage(ACLMessage.REFUSE);
							refusal.addReceiver(receivedMessage.getSender());
							refusal.setProtocol(receivedMessage.getProtocol());
							myAgent.send(refusal);
							
							transitionStatus = highPrice;
						}
					} else if (receivedMessage.getPerformative() == ACLMessage.INFORM) {
						transitionStatus = noBuyers;
					}
				} else {
					block();
				}
				
			}

			@Override
			public boolean done() {
				return hasReceivedMessage;
			}
			
			@Override
			public int onEnd() {
				return transitionStatus;
			}
		}
	}
}
