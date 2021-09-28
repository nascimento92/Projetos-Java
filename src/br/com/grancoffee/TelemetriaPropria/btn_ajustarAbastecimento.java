package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.Timestamp;
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

public class btn_ajustarAbastecimento implements AcaoRotinaJava {

	/**
	 * 21/09/21 vs 1.6 inserido o método salvarNoHistorico e chamaPentaho.
	 */
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {

		Registro[] linhas = arg0.getLinhas();

		String campo = (String) linhas[0].getCampo("AJUSTADO");
		BigDecimal idabast = (BigDecimal) linhas[0].getCampo("ID");
		String status = verificaStatusAbastecimento(idabast);
		Timestamp hora = TimeUtils.getNow();

		if ("S".equals(campo)) {
			arg0.mostraErro("<br/><b>Abastecimento já ajustado!</b><br/>");
		} else if ("1".equals(status)) {
			arg0.mostraErro("<br/><b>Abastecimento Pendente, não pode ser realizado a validação!</b><br/>");
		} else if ("2".equals(status)) {
			arg0.mostraErro("<br/><b>Contagem pendente, não pode ser realizado a validação!</b><br/>");
		} else {
			boolean confirmarSimNao = arg0.confirmarSimNao("Atenção!",
					"Todas as informações digitadas manualmente serão aceitas e o <b>inventário será ajustado</b>, onde não foi digitado manualmente será adotado o que o promotor digitou, continuar?",
					1);

			if (confirmarSimNao) {

				Object idObjeto = linhas[0].getCampo("ID");
				pegarTeclas(idObjeto, arg0, hora);
				salvaResonsavelPeloAjuste(idObjeto, hora);
				salvarNoHistorico(idabast);
				
			}
		}
	}

	private void pegarTeclas(Object idObjeto, ContextoAcao arg0, Timestamp hora) throws Exception {

		int cont = 0;

		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		Collection<?> parceiro = dwfEntityFacade
				.findByDynamicFinder(new FinderWrapper("AD_ITENSRETABAST", "this.ID = ? ", new Object[] { idObjeto }));

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			
			BigDecimal saldoApos = DynamicVO.asBigDecimal("SALDOAPOS");
			BigDecimal saldoEsperado = DynamicVO.asBigDecimal("SALDOESPERADO"); 
			
			if(saldoApos.subtract(saldoEsperado).intValue()!=0) {
				String codbem = DynamicVO.asString("CODBEM");
				String tecla = DynamicVO.asString("TECLA");
				BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
				BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
				BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
				
				BigDecimal valor = saldoApos.subtract(saldoEsperado);
				
				if(codbem==null) {
					codbem = getCodbem(idObjeto);
				}
				
				inserirSolicitacaoDeAjuste(codbem, tecla, produto, capacidade, nivelpar, saldoEsperado, valor,
						idObjeto, hora);
			
				cont++;
			}
						
			DynamicVO.setProperty("AJUSTADO", "S");
			itemEntity.setValueObject((EntityVO) DynamicVO);
		}
		
		if(cont>0) {
			arg0.setMensagemRetorno("Foram ajustado(s) <b>"+cont+"</b> tecla(s).");
		}

	}

	private void inserirSolicitacaoDeAjuste(String codbem, String tecla, BigDecimal produto, BigDecimal capacidade, BigDecimal nivelpar, 
			BigDecimal saldoAnterior, BigDecimal valor, Object idObjeto, Timestamp hora) {
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
			VO.setProperty("SALDOANTERIOR", saldoAnterior);
			VO.setProperty("QTDAJUSTE", valor);
			VO.setProperty("MANUAL", "N");
			VO.setProperty("OBSERVACAO", "Botão Ajustar Abastecimento");
			VO.setProperty("SALDOFINAL", saldoAnterior.add(valor));
			VO.setProperty("IDABASTECIMENTO", idObjeto);
			VO.setProperty("AD_DTSOLICIT", hora);

			dwfFacade.createEntity("GCSolicitAjuste", (EntityVO) VO);

		} catch (Exception e) {
			salvarException("[inserirSolicitacaoDeAjuste] Não foi possivel cadastrar a solicitação de ajuste!"+e.getMessage()+"\n"+e.getCause());
		}
	}

	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
		codUsuLogado = ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID();
		return codUsuLogado;
	}

	private String verificaStatusAbastecimento(Object idabast) {
		String status = "";
		try {

			JapeWrapper DAO = JapeFactory.dao("AD_RETABAST");
			DynamicVO VO = DAO.findOne("ID=?", new Object[] { idabast });
			status = VO.asString("STATUS");

		} catch (Exception e) {
			salvarException("[verificaStatusAbastecimento] Não foi possivel verificar o status!"+e.getMessage()+"\n"+e.getCause());
		}

		return status;
	}

	private void salvaResonsavelPeloAjuste(Object idObjeto, Timestamp hora) {
		try {

			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("AD_RETABAST", "this.ID=?", new Object[] { idObjeto }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("DTVALIDACAO", hora);
				VO.setProperty("STATUSVALIDACAO", "2");
				VO.setProperty("CODUSUVALIDACAO", getUsuLogado());

				itemEntity.setValueObject(NVO);
			}

		} catch (Exception e) {
			salvarException("[salvaResonsavelPeloAjuste] Não foi possivel salvar o responsavel pelo ajuste!"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private String getCodbem(Object idabast) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_RETABAST");
		DynamicVO VO = DAO.findOne("ID=?", new Object[] { idabast });
		String codbem = VO.asString("CODBEM");
		return codbem;
	}
	
	private void salvarException(String mensagem) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("OBJETO", "btn_ajustarAbastecimento");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);
			
			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);
			
		} catch (Exception e) {
			//aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! "+e.getMessage());
		}
	}
	
	private void salvarNoHistorico(BigDecimal idabast) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_HISTRET");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("IDRETORNO", idabast);
			VO.setProperty("DTSOLICI", TimeUtils.getNow());
			VO.setProperty("AJUSTADO", "N");
			
			dwfFacade.createEntity("AD_HISTRET", (EntityVO) VO);
		} catch (Exception e) {
			salvarException("[salvarNoHistorico] nao foi possivel salvar no histórico! "+e.getCause()+"\n"+e.getMessage());
		}
	}
	
}


