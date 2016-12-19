package br.com.caelum.leilao.servico;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.dominio.Leilao;
import br.com.caelum.leilao.infra.dao.RepositorioDeLeiloes;
import br.com.caelum.leilao.infra.email.Carteiro;

public class EncerradorDeLeilaoTest {

	private RepositorioDeLeiloes daoFalso;
	private Carteiro carteiroFalso;
	private EncerradorDeLeilao encerrador;
	private Calendar antiga;
	private Leilao leilao1;
	private Leilao leilao2;
	
	@Before
	public void setUp(){
		daoFalso = mock(RepositorioDeLeiloes.class);
		carteiroFalso = mock(Carteiro.class);
		encerrador = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);
		
		leilao1 = new CriadorDeLeilao().para("Tv de Plasma").naData(antiga).constroi();
		leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();
	}
	
	@Test
	public void deveEncerrarLeiloesQueComecaramUmaSemanaAntes() {

		List<Leilao> leiloesAntigos = Arrays.asList(leilao1, leilao2);

		when(daoFalso.correntes()).thenReturn(leiloesAntigos);

		encerrador.encerra();

		assertEquals(2, encerrador.getTotalEncerrados());
		assertTrue(leilao1.isEncerrado());
		assertTrue(leilao2.isEncerrado());
	}

	@Test
	public void naoDeveEncerrarLeiloesCasoNaoHajaNenhum() {
		when(daoFalso.correntes()).thenReturn(new ArrayList<Leilao>());

		encerrador.encerra();

		assertEquals(0, encerrador.getTotalEncerrados());
	}

	@Test
	public void deveAtualizarLeiloesEncerrados() {
		Leilao leilao1 = new CriadorDeLeilao().para("Tv de Plasma").naData(antiga).constroi();

		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1));

		encerrador.encerra();

		verify(daoFalso, times(1)).atualiza(leilao1);

	}

	@Test
	public void naoDeveEncerrarLeiloesQueComecaramMenosDeUmaSemanaAtras() {
		Calendar ontem = Calendar.getInstance();
		ontem.add(Calendar.DAY_OF_MONTH, -1);

		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(ontem).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(ontem).constroi();

		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

		encerrador.encerra();

		assertEquals(0, encerrador.getTotalEncerrados());
		assertFalse(leilao1.isEncerrado());
		assertFalse(leilao2.isEncerrado());

		verify(daoFalso, never()).atualiza(leilao1);
		verify(daoFalso, never()).atualiza(leilao2);
	}
	
	@Test
	public void deveContinuarAExecucaoMesmoQuandoDaoFalha(){
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
		
		doThrow(new RuntimeException()).when(daoFalso).atualiza(leilao1);
		
		encerrador.encerra();
		
		verify(daoFalso).atualiza(leilao2);
		verify(carteiroFalso).envia(leilao2);
		
		verify(carteiroFalso, times(0)).envia(leilao1);
	}

	@Test
	public void deveEnviarEmailAposPersistirLeilaoEncerrado() {
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1));

		encerrador.encerra();

		InOrder inOrder = inOrder(daoFalso, carteiroFalso);
		inOrder.verify(daoFalso, times(1)).atualiza(leilao1);
		inOrder.verify(carteiroFalso, times(1)).envia(leilao1);
	}
	
	@Test
    public void deveContinuarAExecucaoMesmoQuandoEnviadorDeEmaillFalha() {
        when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

        doThrow(new RuntimeException()).when(carteiroFalso).envia(leilao1);

        encerrador.encerra();

        verify(daoFalso).atualiza(leilao2);
        verify(carteiroFalso).envia(leilao2);
    }
	
	@Test
    public void deveDesistirSeDaoFalhaPraSempre() {
        when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

        doThrow(new RuntimeException()).when(daoFalso).atualiza(any(Leilao.class));

        encerrador.encerra();

        verify(carteiroFalso, never()).envia(any(Leilao.class));
    }
}
