package br.com.caelum.leilao.servico;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.dominio.Leilao;
import br.com.caelum.leilao.dominio.Pagamento;
import br.com.caelum.leilao.dominio.Usuario;
import br.com.caelum.leilao.infra.dao.RepositorioDeLeiloes;
import br.com.caelum.leilao.infra.dao.RepositorioDePagamentos;
import br.com.caelum.leilao.infra.relogio.Relogio;

public class GeradorDePagamentoTest {
	
	private RepositorioDeLeiloes leiloes;
	private RepositorioDePagamentos pagamentos;
	private Leilao leilao;
	private GeradorDePagamento gerador;
	private Relogio relogio;
	
	@Before
	public void setUp(){
		leiloes = mock(RepositorioDeLeiloes.class);
		pagamentos = mock(RepositorioDePagamentos.class);
		relogio = mock(Relogio.class);
		
		leilao = new CriadorDeLeilao().para("Playstation")
				.lance(new Usuario("Mustafa"), 2000.0)
				.lance(new Usuario("Mauricio"), 2500.0)
				.constroi();
	}
	
	@Test
	public void deveGerarPagamentoParaUmLeilaoEncerrado(){
		when(leiloes.encerrados()).thenReturn(Arrays.asList(leilao));
		
		gerador = new GeradorDePagamento(leiloes, pagamentos, new Avaliador());
		gerador.gera();
		
		ArgumentCaptor<Pagamento> argumento = ArgumentCaptor.forClass(Pagamento.class);
		verify(pagamentos).salva(argumento.capture());
		
		Pagamento pagamentoGerado = argumento.getValue();
		
		assertEquals(2500.0, pagamentoGerado.getValor(), 0.00001);
	}
	
	@Test
	public void deveEmpurrarParaOProximoDiaUtil(){
		
		Leilao leilao = new CriadorDeLeilao().para("Playstation")
				.lance(new Usuario("Davi"), 2000)
				.lance(new Usuario("Mauricio"), 2500)
				.constroi();
		
		when(leiloes.encerrados()).thenReturn(Arrays.asList(leilao));
		
		Calendar sabado = Calendar.getInstance();
		sabado.set(2012, Calendar.APRIL, 8);
		
		when(relogio.hoje()).thenReturn(sabado);
		
		gerador = new GeradorDePagamento(leiloes, pagamentos, new Avaliador(), relogio);
		gerador.gera();
		
		ArgumentCaptor<Pagamento> argumento = ArgumentCaptor.forClass(Pagamento.class);
		verify(pagamentos).salva(argumento.capture());
		
		Pagamento pagamentoGerado = argumento.getValue();
		
		assertEquals(Calendar.MONDAY, pagamentoGerado.getData().get(Calendar.DAY_OF_WEEK));
		assertEquals(9, pagamentoGerado.getData().get(Calendar.DAY_OF_MONTH));
	}
}











