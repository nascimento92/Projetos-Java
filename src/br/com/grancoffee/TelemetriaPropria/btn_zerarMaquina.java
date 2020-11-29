package br.com.grancoffee.TelemetriaPropria;

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
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btn_zerarMaquina implements AcaoRotinaJava {
	
	/**
	 * 27/11/20 14:38 vs 1.1 - inserido método para registrar o Throw na tela Exceptions do skw.
	 */
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {

		boolean confirmarSimNao = arg0.confirmarSimNao("ATENÇÃO!", "O sistema preencherá os valores necessários para <b>Zerar a Máquina</b>, continuar?", 1);
		if(confirmarSimNao) {
			start(arg0);
			arg0.setMensagemRetorno("Dados carregados, clicar no botão <b>Finalizar Ajuste</b>!");
		}
	}

	private void start(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		Object id = linhas[0].getCampo("ID");
		Object patrimonio = linhas[0].getCampo("CODBEM");
		Object ajustado = linhas[0].getCampo("AJUSTADO");
		
		if("S".equals(ajustado)) {
			arg0.mostraErro("<b>Erro! Teclas já ajustadas</b>");
		}else {
			zerarTeclas(id, patrimonio);
		}
		
	}

	private void zerarTeclas(Object id, Object patrimonio) {
		try {

			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_ITENSAJUSTESMANUAIS",
					"this.ID=? AND this.CODBEM=? ", new Object[] { id, patrimonio }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;
				
				VO.setProperty("QTDAJUSTE", VO.asBigDecimal("SALDOANTES").negate());

				itemEntity.setValueObject(NVO);
			}

		} catch (Exception e) {
			String msg = "Nao foi possivel zerar a máquina! "+e.getMessage()+"\n"+e.getCause();
			salvarException(msg);
		}
	}
		
	private void salvarException(String mensagem) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("OBJETO", "btn_zerarMaquina");
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

}
