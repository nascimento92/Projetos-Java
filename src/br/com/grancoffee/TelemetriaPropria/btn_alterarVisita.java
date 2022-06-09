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

public class btn_alterarVisita implements AcaoRotinaJava{
	
	int x = 0;
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		
		String motivo = (String) arg0.getParam("MOTIVO");
		String substituto = (String) arg0.getParam("SUB");
		Timestamp data = (Timestamp) arg0.getParam("DATA");
		Timestamp dataAtendimento = (Timestamp) arg0.getParam("DTVISIT");

		Registro[] linhas = arg0.getLinhas();
		
		for(Registro r : linhas) {

			start(r,motivo, new BigDecimal(substituto), data, arg0, dataAtendimento);

		}
		
		if(x>0) {
			arg0.setMensagemRetorno("<br/>Foram alteradas <b>"+x+"</b> visitas!");
		}
	}
	

	private void start(Registro linha,String motivo,BigDecimal substituto, Timestamp data, ContextoAcao arg0, Timestamp dataAtendimento) throws Exception {
		
		validacoes(linha,arg0,data);
		
		BigDecimal numos = (BigDecimal) linha.getCampo("NUMOS");
		
		if(numos!=null) { //já no portal
			
			if(substituto!=null) {
				alterarAtendenteOS(numos, substituto);
			}
			
		}else {
			if(data!=null) {
				linha.setCampo("DTAGENDAMENTO", data);
			}	
		}
		
		linha.setCampo("AD_MOTALT", motivo);
		linha.setCampo("AD_CODUSUALT", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
		linha.setCampo("AD_DTALTAGEND", TimeUtils.getNow());
		
		if(data!=null) {
			linha.setCampo("DTAGENDAMENTO", data);
		}
		
		if(dataAtendimento!=null) {
			linha.setCampo("AD_DTATENDIMENTO", dataAtendimento);
		}

		if(substituto!=null) {
			linha.setCampo("AD_USUSUB", substituto);
		}
		
		x++;
			
	}
	
	private void alterarAtendenteOS(BigDecimal numos, BigDecimal substituto) throws Exception {
		DynamicVO TCSITE = getTcsite(numos);
		BigDecimal codprod = TCSITE.asBigDecimal("CODPROD");
		BigDecimal codserv = TCSITE.asBigDecimal("CODSERV");
		
		cadastraServicoParaOhExecutante(substituto,codprod,codserv);
		insertTcsrus(substituto);
		salvarNovoResponsavel(numos, substituto);
	}
	
	private DynamicVO getTcsite(BigDecimal NumOs) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("ItemOrdemServico");
		DynamicVO VO = DAO.findOne("NUMOS=?", new Object[] { NumOs });
		return VO;
	}
	
	private void cadastraServicoParaOhExecutante(BigDecimal usuario, BigDecimal produto, BigDecimal servico) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("ServicoProdutoExecutante");
			DynamicVO VO = (DynamicVO) NPVO;
			VO.setProperty("CODSERV", servico);
			VO.setProperty("CODUSU", usuario);
			VO.setProperty("CODPROD", produto);
			dwfFacade.createEntity("ServicoProdutoExecutante", (EntityVO) VO);
		} catch (Exception e) {
			/*
			 * salvarException("[cadastraServicoParaOhExecutante] nfoi cadastrar o servi" +
			 * servico + " para o executante:" + usuario + "\n" + e.getMessage() + "\n" +
			 * e.getCause());
			 */
		}
	}
	
	private void insertTcsrus(BigDecimal codusurel) throws Exception {
	    try {
	      EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
	      EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("RelacionamentoUsuario");
	      DynamicVO VO = (DynamicVO)NPVO;
	      VO.setProperty("CODUSU", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
	      VO.setProperty("CODUSUREL", codusurel);
	      VO.setProperty("TIPO", "G");
	      VO.setProperty("VINCULO", "S");
	      VO.setProperty("LIDERIMEDIATO", "N");
	      dwfFacade.createEntity("RelacionamentoUsuario", (EntityVO)VO);
	    } catch (Exception e) {
			/*
			 * salvarException("[insertTcsrus] não foi possível salvar na tcsrus usuario: "
			 * +codusu+"\n"+e.getMessage()+"\n"+e.getCause());
			 */
	    } 
	  }
	
	public void salvarNovoResponsavel(BigDecimal NumOS, BigDecimal NovoResp) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> itensOS = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("ItemOrdemServico", "this.NUMOS=?", new Object[] { NumOS }));
			for (Iterator<?> Iterator = itensOS.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;
				VO.setProperty("CODUSU", NovoResp);
				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			/*
			 * salvarException("[salvarNovoResponsavel] Não foi possível salvar o novo responsável a OS:"
			 * + NumOS + "\n" + e.getMessage() + "\n" + e.getCause());
			 */
		}
	}
	
	private void validacoes(Registro linha, ContextoAcao arg0, Timestamp data) throws Exception {
		String status = (String) linha.getCampo("STATUS");
		//BigDecimal os = (BigDecimal) linha.getCampo("NUMOS");
		
		if("3".equals(status)) {
			arg0.mostraErro("<br/><b>ATENÇÃO</b><br/><br/> Visita Concluída ! não pode ser alterada !<br/><br/>");
		}
		
		if("4".equals(status)) {
			arg0.mostraErro("<br/><b>ATENÇÃO</b><br/><br/> Visita Cancelada ! não pode ser alterada !<br/><br/>");
		}
		
		/*
		 * if(data.before(TimeUtils.getNow())) { arg0.
		 * mostraErro("<br/><b>ATENÇÃO</b><br/><br/> A data não pode ser inferior que a data atual !<br/><br/>"
		 * ); }
		 */
		
		/*
		 * if(os != null) { arg0.
		 * mostraErro("<br/><b>ATENÇÃO</b><br/><br/> Só é possível alterar uma visita agendada ! a visita solicitada já gerou a OS: <b>"
		 * +os+"</b> não poderá ser alterada !<br/><br/>"); }
		 */
	}

}
