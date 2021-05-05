package br.com.flow.trocaDeGrade;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
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

public class flow_t_grade_TarefaJava_AlteraContrato implements TarefaJava {

	@Override
	public void executar(ContextoTarefa arg0) throws Exception {
		Object idFlow = arg0.getIdInstanceProcesso();
		String patrimonio = getPatrimonio(idFlow);
		deletarTeclasContrato(patrimonio);
		getTeclas(idFlow, patrimonio);
		verificaNecessidadeDePecas(idFlow);
	}

	public String getPatrimonio(Object idFlow) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_MAQUINASTGRADE");
		DynamicVO VO = DAO.findOne("IDINSTPRN=?", new Object[] { idFlow });
		String patrimonio = VO.asString("CODBEM");

		return patrimonio;
	}

	public void deletarTeclasContrato(String patrimonio) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("teclas", "this.CODBEM=?", new Object[] { patrimonio }));
		} catch (Exception e) {
			salvarException("[deletarTeclasContrato] Nao foi possivel excluir as teclas, patrimonio: "+patrimonio+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}

	public void getTeclas(Object idFlow, String patrimonio) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("AD_GRADEFUTURA", "this.IDINSTPRN = ? ", new Object[] { idFlow }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);
				inserirTecla(DynamicVO, patrimonio);
			}

		} catch (Exception e) {
			salvarException("[getTeclas] Nao foi possivel obter as teclas, idFlow: "+idFlow+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}

	public void inserirTecla(DynamicVO DynamicVO, String patrimonio) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("teclas");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("TECLA", new BigDecimal(DynamicVO.asString("TECLA")));
			VO.setProperty("CODPROD", DynamicVO.asBigDecimal("CODPROD"));
			VO.setProperty("NUMCONTRATO", getContrato(patrimonio));
			VO.setProperty("VLRPAR", DynamicVO.asBigDecimal("VLRPARC"));
			VO.setProperty("VLRFUN", DynamicVO.asBigDecimal("VLRFUN"));
			VO.setProperty("AD_CAPACIDADE", DynamicVO.asBigDecimal("CAPACIDADE"));
			VO.setProperty("AD_NIVELPAR", DynamicVO.asBigDecimal("NIVELPAR"));
			VO.setProperty("AD_NIVELALERTA", DynamicVO.asBigDecimal("NIVELALERTA"));

			dwfFacade.createEntity("teclas", (EntityVO) VO);
		} catch (Exception e) {
			salvarException("[inserirTecla] Nao foi possivel inserir as teclas, codbem: "+patrimonio+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	public void verificaNecessidadeDePecas(Object idFlow) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("InstanciaVariavel",
					"this.IDINSTPRN = ? and this.NOME=? ", new Object[] { idFlow, "PECA_SEPARADA" }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);

				String texto = (String) DynamicVO.getProperty("TEXTO");

				if ("1".equals(texto)) {
					String patrimonio = getPatrimonioPecas(idFlow);
					setarPecasNoProximoAbastecimento(patrimonio, idFlow);
				}

			}

		} catch (Exception e) {
			salvarException("[verificaNecessidadeDePecas] Nao foi possivel verificar se tem pecas idflow: "+idFlow+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void setarPecasNoProximoAbastecimento(String patrimonio, Object idFlow) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("GCInstalacao",
					"this.CODBEM=?", new Object[] { patrimonio}));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("AD_RETIRARPECAS", "S");

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			salvarException("[setarPecasNoProximoAbastecimento] Nao foi possivel setar que no próximo abastecimento existem peças para serem retiradas, patrimonio: "+patrimonio+" flow: "+idFlow+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private String getPatrimonioPecas(Object idFlow) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_MAQUINASTGRADE");
		DynamicVO VO = DAO.findOne("IDINSTPRN=?",new Object[] { idFlow });
		String patrimonio = VO.asString("CODBEM");
		return patrimonio;
	}
	
	private BigDecimal getContrato(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		BigDecimal contrato = VO.asBigDecimal("NUMCONTRATO");
		return contrato;
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "flow_t_grade_TarefaJava_AlteraContrato");
			VO.setProperty("PACOTE", "br.com.flow.trocaDeGrade");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}
}
