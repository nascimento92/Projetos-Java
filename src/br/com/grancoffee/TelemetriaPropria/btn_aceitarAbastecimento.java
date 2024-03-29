package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btn_aceitarAbastecimento implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		
		Registro[] linhas = arg0.getLinhas();
		
		String campo = (String) linhas[0].getCampo("AJUSTADO");
		BigDecimal idabast = (BigDecimal) linhas[0].getCampo("IDABASTECIMENTO");
		String status = verificaStatusAbastecimento(idabast);
		String ajusteManual = verificaSeEhUmAjusteManual(idabast);

		if ("S".equals(campo)) {
			arg0.mostraErro("<br/><b>Abastecimento j� ajustado!</b><br/>");
		}else if("1".equals(status)) {
			arg0.mostraErro("<br/><b>Abastecimento Pendente, n�o pode ser realizado a valida��o!</b><br/>");
		}else if("2".equals(status)) {
			arg0.mostraErro("<br/><b>Contagem pendente, n�o pode ser realizado a valida��o!</b><br/>");
		}else if("S".equals(ajusteManual)) {
			arg0.mostraErro("<br/><b>Ajuste Manual, utilizar o bot�o na parte superior, Finalizar Ajuste!</b><br/>");
		}else {
			boolean confirmarSimNao = arg0.confirmarSimNao("Aten��o!", "Todas as informa��es digitadas pelo promotor ser�o aceitas e o <b>invent�rio ser� ajustado</b> de acordo com as diferen�as, continuar?", 1);	
			
			if(confirmarSimNao) {
				
				Object idObjeto = linhas[0].getCampo("IDABASTECIMENTO");
				pegarTeclas(idObjeto,arg0);
				salvaResonsavelPeloAjuste(idObjeto);
		
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
			DynamicVO.setProperty("OBSAJUSTE", "Abast. Aceito");
			itemEntity.setValueObject((EntityVO) DynamicVO);
		}
		
		if(cont>0) {
			arg0.setMensagemRetorno("Ajuste Agendado!");
		}else {
			arg0.setMensagemRetorno("N�o existem diferen�as para serem ajustadas!");
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
			VO.setProperty("OBSERVACAO", "Bot�o aceitar abastecimento.");
			VO.setProperty("SALDOFINAL", saldoAtual.add(diferenca));
			VO.setProperty("IDABASTECIMENTO", idabast);
			
			dwfFacade.createEntity("GCSolicitAjuste", (EntityVO) VO);
			
		} catch (Exception e) {
			System.out.println("## [btn_aceitarAbastecimento] ## - N�o foi possivel cadastrar a solicita��o de ajuste!");
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
	
	private String verificaStatusAbastecimento(Object idabast) {
		String status = "";
		try {
			
			JapeWrapper DAO = JapeFactory.dao("GCControleAbastecimento");
			DynamicVO VO = DAO.findOne("ID=?",new Object[] { idabast });
			status = VO.asString("STATUS");
			
			
		} catch (Exception e) {
			System.out.println("## [btn_aceitarAbastecimento] ## - N�o foi possivel verificar o status");
			e.getCause();
			e.getMessage();
			e.printStackTrace();
		}
		
		return status;
	}
	
	private void salvaResonsavelPeloAjuste(Object idObjeto) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("GCControleAbastecimento",
					"this.ID=?", new Object[] { idObjeto }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("DTVALIDACAO", TimeUtils.getNow());
				VO.setProperty("STATUSVALIDACAO", "2");
				VO.setProperty("CODUSUVALIDACAO", getUsuLogado());

				itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			System.out.println("## [btn_aceitarAbastecimento] ## - N�o foi possivel salvar o responsavel pelo ajuste.");
			e.getCause();
			e.getMessage();
			e.printStackTrace();
		}
	}
	
	private String verificaSeEhUmAjusteManual(BigDecimal idabast) {
		String status = "N";
		try {
			
			JapeWrapper DAO = JapeFactory.dao("GCControleAbastecimento");
			DynamicVO VO = DAO.findOne("ID=?",new Object[] { idabast });
			String tipoajuste = VO.asString("AJUSTEMANUAL");
			
			if("S".equals(tipoajuste)) {
				status = tipoajuste;
			}
			
			
		} catch (Exception e) {
			System.out.println("## [btn_aceitarAbastecimento] ## - N�o foi possivel verificar se o ajuste � manual");
			e.getCause();
			e.getMessage();
			e.printStackTrace();
		}
		
		return status;
	}

}


