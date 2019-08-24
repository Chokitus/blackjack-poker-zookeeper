package java_zookeeper.blackjack;

import java.io.IOException;

import org.apache.zookeeper.KeeperException;

import java_zookeeper.blackjack.game.player.Dealer;
import java_zookeeper.blackjack.game.player.Player;
import java_zookeeper.blackjack.game.service.BlackjackGameService;
import java_zookeeper.blackjack.zookeeper.ZookeeperPlayerRegister;
import java_zookeeper.blackjack.zookeeper.ZookeeperService;
import lombok.extern.log4j.Log4j;

@Log4j
public class BlackJack {

	public static void main(final String[] args) throws IOException, InterruptedException, KeeperException {
		ZookeeperService.createInstance("localhost:2181");
		if (args[0].equals("dealer")) {
			Dealer dealer = ZookeeperPlayerRegister.registerDealer("001", "NomeDoDealer", 3);
			new BlackJack().playDealerGame(dealer);
		} else {
			Player player = ZookeeperPlayerRegister.registerPlayer("001", "NomeDoPlayer");
			new BlackJack().playPlayerGame(player);
		}
	}

	private void playPlayerGame(final Player player) throws KeeperException, InterruptedException {
		while (player.getCurrentMoney() != 0) {
			/*
			 * Primeiro passo: responder ao pedido de apostas do dealer
			 */
			BlackjackGameService.bet(player);
			/*
			 * Segundo passo: esperar distribuição das cartas
			 */
			BlackjackGameService.waitForCards(player, 2);
			/*
			 * Terceiro passo: espera até que um pedido por ação chegue
			 */
			BlackjackGameService.waitUntilAskedForActions(player);
			/*
			 * Quarto passo: Olha para a mão das outras pessoas (e, em especial,
			 * do Dealer)
			 */
			BlackjackGameService.seeTable(player);
			/*
			 * Quinto passo: escolher uma (ou mais) ação(ões)
			 */
			BlackjackGameService.act(player);
			/*
			 * Sexto passo: esperar Dealer anunciar fim da rodada
			 */
			BlackjackGameService.waitUntilEndOfRound(player);
			/*
			 * Sétimo passo: ver se ganhamos
			 */
			BlackjackGameService.verifyPlayerResults(player);
			player.printHand();
			BlackJack.log.info(player.getScore());
			if (player.getCurrentMoney() <= 0) {
				return;
			}
			/*
			 * Oitavo passo: resetar
			 */
			player.newRound();
			/*
			 * Nono passo: se registrar para o próximo round
			 */
			BlackjackGameService.registerForNextRound(player);
			/*
			 * Nono passo: aguardar Dealer anunciar novo round
			 */
			BlackjackGameService.waitUntilNextRound(player);
		}
	}

	private void playDealerGame(final Dealer dealer) throws KeeperException, InterruptedException {
		while (!dealer.getListOfPlayers().isEmpty()) {
			/**
			 * Primeiro passo: Pedir apostas
			 */
			BlackjackGameService.askForBet(dealer);
			/**
			 * Segundo passo: distribuir cartas
			 */
			BlackjackGameService.distributeCards(dealer);
			/**
			 * Terceiro passo: dar carta para si mesmo
			 */
			BlackjackGameService.sendCardToMyself(dealer);
			/**
			 * Quarto passo: perguntar ação, agora que todos estão com as
			 * cartas. As ações possíveis são: Dobrar (que consiste em dobrar
			 * sua aposta e pegar uma carta), Pedir carta, Parar e Desistir *
			 */
			BlackjackGameService.askForActions(dealer);
			/*
			 * Quinto passo: O Dealer, após todos os jogadores estarem
			 * confortáveis com suas mãos, completa sua mão até dar 17
			 */
			BlackjackGameService.fillHandUntilMinimum(dealer);
			/*
			 * Sexto passo: Todos os players finalizaram sua mão, o Dealer
			 * completou sua mão até 17 (no mínimo), e assim, o Dealer deverá
			 * verificar os ganhadores, pagando-os ou pegando suas apostas.
			 */
			BlackjackGameService.verifyWinnersAndDoPayouts(dealer);
			if (dealer.getListOfPlayers().isEmpty()) {
				return;
			}
			/*
			 * Aqui devemos resetar
			 */
			BlackjackGameService.cleanTableForNextRound(dealer);
			/*
			 * Sétimo passo: espera até que todos os Players se registrem para o
			 * próximo round.
			 */
			BlackjackGameService.waitUntilAllPlayersAreReady(dealer);

		}
	}
}
