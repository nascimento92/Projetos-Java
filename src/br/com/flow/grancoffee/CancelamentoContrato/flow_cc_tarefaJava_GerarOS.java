package br.com.flow.grancoffee.CancelamentoContrato;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import com.sankhya.util.TimeUtils;
import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class flow_cc_tarefaJava_GerarOS implements TarefaJava {
	int contador = 0;
	
	@Override
	public void executar(ContextoTarefa arg0) throws Exception {
		start(arg0);		
	}
	
	private void start(ContextoTarefa arg0) {
		Object idflow = arg0.getIdInstanceProcesso();
		verificaPlantas(idflow,arg0);
	}
	
	private void verificaPlantas(Object idflow,ContextoTarefa arg0) {
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT DISTINCT idplanta FROM AD_PATCANCELAMENTO WHERE IDINSTPRN="+idflow);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				BigDecimal planta = contagem.getBigDecimal("idplanta");
				if (planta!=null) {
					criarOsParaAhPlanta(idflow,planta,arg0);
				}
			}
			
		} catch (Exception e) {
			System.out.println("## [flow_cc_tarefaJava_GerarOS] ## - Não foio possivel determinar as plantas!");
			e.getCause();
			e.getMessage();
			e.printStackTrace();
		}
	}
	
	private void criarOsParaAhPlanta(Object idflow,BigDecimal planta,ContextoTarefa arg0) {
		Object usuarioInclusao = arg0.getCampo("SYS_USUARIOINCLUSAO");
		
		String quantidadeMaquinas="Quantidade Máquinas: "+contagemQuantidadeDeMaquinas(idflow,planta)+"\n";
		String Patrimonios = "Patrimônios: \n"+getPatrimonios(idflow,planta)+"\n";
		
		String outrosCampos = getOutrosCampos(idflow,planta,usuarioInclusao);
		
		String descricao = quantidadeMaquinas+Patrimonios+outrosCampos;
		arg0.setCampo("SYS_OS", descricao);
	}
	
	private String getPatrimonios(Object idflow,BigDecimal planta) {
		String patrimonios = "";
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_PATCANCELAMENTO","this.IDINSTPRN = ? and this.IDPLANTA=? ", new Object[] { idflow,planta }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) 
			{
			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			String codbem = (String) DynamicVO.getProperty("CODBEM");
			BigDecimal codprod = (BigDecimal) DynamicVO.getProperty("CODPROD");
			
			patrimonios+=codbem+" - "+getTgfpro(codprod).asString("DESCRPROD")+"\n";
			}
			
			
		} catch (Exception e) {
			System.out.println("## [flow_cc_tarefaJava_GerarOS] ## - Não foio possivel obter os patrimonios!");
			e.getCause();
			e.getMessage();
			e.printStackTrace();
		}
		
		return patrimonios;
	}
	
	private DynamicVO getTgfpro(BigDecimal produto) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?",new Object[] { produto });
		return VO;
	}
	
	private String getOutrosCampos(Object idflow, BigDecimal planta,Object usuarioInclusao) { 
		String descricao = "";
		try {
			
			JapeWrapper DAO = JapeFactory.dao("AD_FORMCANCELAMENTO");
			DynamicVO VO = DAO.findOne("IDINSTPRN=?",new Object[] { idflow });
			if(VO!=null) {
				String multa ="";
				if("1".equals(VO.asString("COBRARMULTA"))) {
					multa = "Multa: SIM - "+VO.asBigDecimal("MULTA")+"\n";
				}else {
					multa = "Multa: NÃO - "+VO.asString("JUSTIFICATIVAMULTA")+"\n";
				}
				String ultimaCobranca ="Última Cobrança: "+ VO.asString("ULTIMACOBRANCA")+"\n";
				String dtRetirada ="Data Retirada: "+ TimeUtils.formataDDMMYY(VO.asTimestamp("DTRETIRADA"))+"\n";
				String endereco ="Endereço: "+ getAdPlanta(VO.asBigDecimal("NUMCONTRATO"),planta).asString("ENDPLAN")+"\n";
				String integracao = "";
				if("1".equals(VO.asString("INTEGRACAO"))) {
					integracao = "Necessário Integração: SIM \n";
				}else {
					integracao = "Necessário Integração: NÃO \n";
				}
				String restricaoHorario="";
				if("1".equals(VO.asString("RESTRICAOHORARIO"))) {
					restricaoHorario = "Possui Restrições de Horário: SIM - "+VO.asString("DESCRRESTRICAO")+"\n";
				}else {
					restricaoHorario = "Possui Restrições de Horário: NÃO \n";
				}
				String acessorios = "";
				if("1".equals(VO.asString("RETIRAACESSORIOS"))) {
					acessorios = "Retirada de Acessórios: SIM - "+VO.asString("ACESSORIOS")+"\n";
				}else {
					acessorios = "Retirada de Acessórios: NÃO \n";
				}
				String contato= "Contato: "+VO.asString("CT_NOME")+", "+VO.asString("CT_TEL")+"\n";
				String email = "E-mail: "+VO.asString("CT_EMAIL")+"\n";
				String gestor = "Gestor do Contrato: "+usuarioInclusao(usuarioInclusao)+"\n";
				
				descricao=multa+ultimaCobranca+dtRetirada+endereco+integracao+restricaoHorario+acessorios+contato+email+gestor;
			}
			
		} catch (Exception e) {
			System.out.println("## [flow_cc_tarefaJava_GerarOS] ## - Não foio possivel obter as demais descricoes!");
			e.getCause();
			e.getMessage();
			e.printStackTrace();
		}
		
		return descricao;
	}
	
	private int contagemQuantidadeDeMaquinas(Object idflow, BigDecimal planta) {
		int qtd = 0;
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT COUNT(*) AS QTD FROM AD_PATCANCELAMENTO WHERE IDINSTPRN="+idflow+" AND IDPLANTA="+planta);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				qtd = contagem.getInt("QTD");
			}
			
		} catch (Exception e) {
			System.out.println("## [flow_cc_tarefaJava_GerarOS] ## - Não foio possivel determinar as plantas!");
			e.getCause();
			e.getMessage();
			e.printStackTrace();
		}
		
		return qtd;
	}
	
	private DynamicVO getAdPlanta(BigDecimal contrato, BigDecimal planta) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("PLANTAS");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=? and ID=?",new Object[] { contrato,planta });	
		return VO;
	}
		
	private String usuarioInclusao(Object usuarioInclusao) throws Exception {
		String nomeUsuario = "";
		if(usuarioInclusao!=null) {
			JapeWrapper DAO = JapeFactory.dao("Usuario");
			DynamicVO VO = DAO.findOne("CODUSU=?",new Object[] { usuarioInclusao });
			nomeUsuario = VO.asString("NOMEUSUCPLT");
		}
		return nomeUsuario;
	}

}
