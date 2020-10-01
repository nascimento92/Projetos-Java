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

public class btn_recusarAbastecimento implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();

		String campo = (String) linhas[0].getCampo("AJUSTADO");
		BigDecimal idabast = (BigDecimal) linhas[0].getCampo("IDABASTECIMENTO");
		String status = verificaStatusAbastecimento(idabast);
		String ajusteManual = verificaSeEhUmAjusteManual(idabast);

		if ("S".equals(campo)) {
			arg0.mostraErro("<br/><b>Abastecimento já ajustado!</b><br/>");
		}else if("1".equals(status)) {
			arg0.mostraErro("<br/><b>Abastecimento Pendente, não pode ser realizado a validação!</b><br/>");
		}else if("2".equals(status)) {
			arg0.mostraErro("<br/><b>Contagem pendente, não pode ser realizado a validação!</b><br/>");
		}else if("S".equals(ajusteManual)) {
			arg0.mostraErro("<br/><b>Ajuste Manual, utilizar o botão na parte superior, Finalizar Ajuste!</b><br/>");
		}else {
			boolean confirmarSimNao = arg0.confirmarSimNao("Atenção!",
					"Todas as informações digitadas pelo promotor serão <b>recusadas</b> e o inventário <b>não será ajustado</b>, continuar?",
					1);

			if (confirmarSimNao) {

				Object idObjeto = linhas[0].getCampo("IDABASTECIMENTO");
				pegarTeclas(idObjeto, arg0);
				salvaResonsavelPeloAjuste(idObjeto);
				arg0.setMensagemRetorno("Finalizado!");
			}
		}
	}
	
	private void pegarTeclas(Object idObjeto,ContextoAcao arg0) throws Exception {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("GCItensAbastecimento", "this.IDABASTECIMENTO = ? ", new Object[] { idObjeto }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				
				DynamicVO.setProperty("QTDAJUSTE", new BigDecimal(0));
				DynamicVO.setProperty("AJUSTADO", "S");
				DynamicVO.setProperty("OBSAJUSTE", "Abast. Recusado");
				itemEntity.setValueObject((EntityVO) DynamicVO);
			}
			
		} catch (Exception e) {
			System.out.println("## [btn_recusarAbastecimento] ## - Não foi possivel salvar as informações nas teclas");
			e.getCause();
			e.getMessage();
			e.printStackTrace();
		}
	}
	
	private String verificaStatusAbastecimento(BigDecimal idabast) {
		String status = "";
		try {
			
			JapeWrapper DAO = JapeFactory.dao("GCControleAbastecimento");
			DynamicVO VO = DAO.findOne("ID=?",new Object[] { idabast });
			status = VO.asString("STATUS");
			
			
		} catch (Exception e) {
			System.out.println("## [btn_recusarAbastecimento] ## - Não foi possivel verificar o status");
			e.getCause();
			e.getMessage();
			e.printStackTrace();
		}
		
		return status;
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
			System.out.println("## [btn_recusarAbastecimento] ## - Não foi possivel verificar se o ajuste é manual");
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
			System.out.println("## [btn_recusarAbastecimento] ## - Não foi possivel salvar o responsavel pelo ajuste.");
			e.getCause();
			e.getMessage();
			e.printStackTrace();
		}
	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
	    codUsuLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
	    return codUsuLogado;    	
	}

}
