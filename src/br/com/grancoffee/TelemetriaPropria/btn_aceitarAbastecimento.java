package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btn_aceitarAbastecimento implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		
		Registro[] linhas = arg0.getLinhas();
		
		String campo = (String) linhas[0].getCampo("AJUSTADO");
		
		if("S".equals(campo)) {
			arg0.mostraErro("Abastecimento já ajustado!");
		}else {
			boolean confirmarSimNao = arg0.confirmarSimNao("Atenção!", "Todas as informações digitadas pelo promotor serão aceitas e o <b>inventário será ajustado</b> de acordo com as diferenças, continuar?", 1);	
			
			if(confirmarSimNao) {
				
				Object idObjeto = linhas[0].getCampo("IDABASTECIMENTO");
				pegarTeclas(idObjeto,arg0);
		
			}
		}
	}
	
	private void pegarTeclas(Object idObjeto,ContextoAcao arg0) throws Exception {
		
		int cont=0;
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		Collection<?> parceiro = dwfEntityFacade
				.findByDynamicFinder(new FinderWrapper("GCItensAbastecimento", "this.IDABASTECIMENTO = ? ", new Object[] { idObjeto }));

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			
			BigDecimal diferenca = DynamicVO.asBigDecimal("DIFERENCA");
			BigDecimal idabast = DynamicVO.asBigDecimal("IDABASTECIMENTO");
			
			if(diferenca.intValue()!=0) {
				String tecla = DynamicVO.asString("TECLA");
				BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
				BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
				BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
				BigDecimal saldoAtual = DynamicVO.asBigDecimal("SALDOATUAL");
				String codbem = DynamicVO.asString("CODBEM");
				
				inserirSolicitacaoDeAjuste(tecla,produto,capacidade,nivelpar,saldoAtual,codbem,diferenca,idabast);
				cont++;
			}
			
			DynamicVO.setProperty("QTDAJUSTE", new BigDecimal(0));
			DynamicVO.setProperty("AJUSTADO", "S");
			itemEntity.setValueObject((EntityVO) DynamicVO);
		}
		
		if(cont>0) {
			arg0.setMensagemRetorno("Ajuste Agendado!");
		}else {
			arg0.setMensagemRetorno("Não existem diferenças para serem ajustadas!");
		}
	}
	
	private void inserirSolicitacaoDeAjuste(String tecla,BigDecimal produto,BigDecimal capacidade,BigDecimal nivelpar,BigDecimal saldoAtual,String codbem,BigDecimal diferenca,BigDecimal idabast) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCSolicitAjuste");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODBEM", codbem);
			VO.setProperty("CODUSU", getUsuLogado());
			VO.setProperty("TECLA", tecla);
			VO.setProperty("CODPROD", produto);
			VO.setProperty("CAPACIDADE", capacidade);
			VO.setProperty("NIVELPAR", nivelpar);
			VO.setProperty("SALDOANTERIOR", saldoAtual);
			VO.setProperty("QTDAJUSTE", diferenca);
			VO.setProperty("MANUAL", "N");
			VO.setProperty("OBSERVACAO", "Botão aceitar abastecimento.");
			VO.setProperty("SALDOFINAL", saldoAtual.add(diferenca));
			//VO.setProperty("IDABASTECIMENTO", idabast);
			
			dwfFacade.createEntity("GCSolicitAjuste", (EntityVO) VO);
			
		} catch (Exception e) {
			System.out.println("## [btn_aceitarAbastecimento] ## - Não foi possivel cadastrar a solicitação de ajuste!");
			e.getMessage();
			e.getCause();
			e.printStackTrace();
		}
	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
	    codUsuLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
	    return codUsuLogado;    	
	}

}


