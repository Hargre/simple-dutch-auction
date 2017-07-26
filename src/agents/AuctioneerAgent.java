package agents;

import java.util.ArrayList;
import java.util.List;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class AuctioneerAgent extends Agent {
	private static final long serialVersionUID = 667090917660533415L;
	private static final double RESERVE_RATE = 0.4;
	
	private double initialPrice;
	private double reservePrice;
	
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
		
		getInitialPrice();
		
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
	
	private void getInitialPrice() {
		Object args[] = getArguments();
		
		if (args != null && args.length >= 1) {
			try {
				initialPrice = Double.parseDouble(args[0].toString());
				setReservePrice();
			} catch (NumberFormatException ex) {
				System.out.println("Please type the initial price for the auction, in decimal form.");
				doDelete();
			}
		} else {
			System.out.println("Initial price not determined, terminating agent...");
			doDelete();
		}
	}
	
	private void setReservePrice() {
		reservePrice = initialPrice * RESERVE_RATE;
	}
	
	/*
	 * The agent will operate as a Finite State Machine (FSM).
	 * Each state is represented by a internal Behaviour.
	 */
	private class AuctioneerBehaviour extends FSMBehaviour {
		private static final long serialVersionUID = -7791885794766458598L;
		
		private double currentPrice;
		private double reductionRate;
		private List<AID> buyers;
		
		public AuctioneerBehaviour() {
			currentPrice = initialPrice;
			reductionRate = (initialPrice - reservePrice) * 0.1;
			buyers = new ArrayList<>();
			
			registerFirstState(new SearchBuyersBehaviour(), "searching for buyers");
			registerState(new InformBuyersBehaviour(), "inform buyers");
			registerState(new CallBuyersBehaviour(), "call for proposal");
			
			/* Empty state only to simulate transition. 
			 * Will be removed when proper end states are implemented.
			 */
			registerLastState(new OneShotBehaviour() {
				private static final long serialVersionUID = 3438209180132381071L;

				@Override
				public void action() {
				}
			}, "ending auction");
			
			registerDefaultTransition("searching for buyers", "inform buyers");
			registerDefaultTransition("inform buyers", "call for proposal");
			registerDefaultTransition("call for proposal", "ending auction");
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
		
		/*
		 * Informs existing buyers that the auction is about to begin.
		 */
		private class InformBuyersBehaviour extends OneShotBehaviour {
			private static final long serialVersionUID = -4608982946670101683L;

			@Override
			public void action() {
				ACLMessage message = new ACLMessage(ACLMessage.INFORM);
				message.setProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION);
				message.setContent("begin-auction");
				
				for (AID buyer : buyers) {
					message.addReceiver(buyer);
				}
				myAgent.send(message);
				
				System.out.println("The auction is about to begin...");
			}
		}
		
		/*
		 * Sends a call for proposal (CFP) to existing buyers.
		 */
		private class CallBuyersBehaviour extends OneShotBehaviour {
			private static final long serialVersionUID = 4938234929644296938L;

			@Override
			public void action() {
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				cfp.setProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION);
				cfp.setContent(Double.toString(currentPrice));
				
				for (AID buyer : buyers) {
					cfp.addReceiver(buyer);
					System.out.println("Sending CFP to [" + buyer.getLocalName() +"]");
				}
				myAgent.send(cfp);
			}
		}
	}
}
