package br.com.flow.prod.RelatorioInstalacao;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class flow_rel_inst_btn_CarregaDados implements AcaoRotinaJava {
	
	/**
	 * Botão usado para carregar os dados do flow.
	 * 
	 * @author gabriel.nascimento
	 * @Versão 1.0
	 */
	
	private BigDecimal idEmail = null;
	
	public void doAction(ContextoAcao arg0) throws Exception {
		start(arg0);
	}
	
	//1.0
	private void start(ContextoAcao arg0) throws Exception {
		try {
			Registro[] linhas = arg0.getLinhas();
			
			for(int i=0; i<linhas.length; i++) {
				BigDecimal idflow = (BigDecimal) linhas[i].getCampo("IDFLOW");
				
				contratos(idflow,linhas[i]);
				cadastroUm(idflow, linhas[i]);
				cadastroNespresso(idflow, linhas[i]);
				Patrimonios(idflow, linhas[i]);
				producao(idflow, linhas[i]);
				vendas(idflow, linhas[i]);
				planejamento(idflow, linhas[i]);
				FaturamentoRemessa(idflow, linhas[i]);
				logistica(idflow, linhas[i]);
				AtivacaoLocacao(idflow, linhas[i]);
				faturamentoContrato(idflow, linhas[i]);
				buscaDadosSolicitante(idflow, linhas[i]);
				verificaAnexos(idflow, linhas[i]);
				 	
			}
			
			arg0.setMensagemRetorno("DADOS CARREGADOS!");
		} catch (Exception e) {
			System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO CARREGAR OS DADOS "+e.getMessage());
			e.getStackTrace();
		}
	}
	
	//CONTRATOS --------------------------------------------------------------------
	
	//1.1
	private void contratos(BigDecimal idflow, Registro linhas) {
		
		String tarefaContratos = "UserTask_1jgv6gi";
		
		List<String> camposContratos = new ArrayList<String>();
		camposContratos.add("CT_UNIDADE");
		camposContratos.add("CT_APROVADO");
		camposContratos.add("CT_CODCENCUS");
		camposContratos.add("CT_TEMPEDIDO");
		camposContratos.add("CT_VALORLOCACAO");
		camposContratos.add("CT_CONSULTOR");
		camposContratos.add("CT_ANTECIPAR");
		camposContratos.add("CT_MOTIVOATRASO");
		camposContratos.add("CT_MOTIVORECUSA");
		camposContratos.add("CT_OBSERVACAO");
		camposContratos.add("CT_DTINSTALACAO");
		
		for(String campo:camposContratos) {
			carregaDadosContratos(idflow,campo,linhas,tarefaContratos);
		}	
		
		String responsavel = "CT_RESPONSAVEL";
		String criacao = "CT_DHCRIACAO";
		String aceite = "CT_DHACEITE";
		String conclusao = "CT_DHCONCLUSAO";
		String setor = "contratos";		
		carregaDados(idflow,tarefaContratos,linhas,responsavel,criacao,aceite,conclusao,setor);
	}
	
	//1.1.1
	private void carregaDadosContratos(BigDecimal idFlow, String nomeCampo,Registro linha,String tarefaContratos) {
		try {

			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("InstanciaVariavel", "this.IDINSTPRN = ? AND this.NOME=? ", new Object[] { idFlow,nomeCampo }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

				if(DynamicVO!=null) {

					String texto = DynamicVO.asString("TEXTO");
					Timestamp data = DynamicVO.asTimestamp("DTA");
					BigDecimal valor = DynamicVO.asBigDecimal("NUMDEC");
					String textoLongo = DynamicVO.asString("TEXTOLONGO");
					BigDecimal inteiro = DynamicVO.asBigDecimal("NUMINT");
					
					if("CT_CODCENCUS".equals(nomeCampo)) {
						linha.setCampo(nomeCampo, new BigDecimal(texto));
					}else if("CT_CONSULTOR".equals(nomeCampo)){
						linha.setCampo(nomeCampo, new BigDecimal(texto));
					}else {
						if(texto!=null) {
							linha.setCampo(nomeCampo, texto);
						}else if(data!=null) {
							linha.setCampo(nomeCampo, data);
						}else if(valor!=null) {
							linha.setCampo(nomeCampo, valor);
						}else if (textoLongo!=null) {
							linha.setCampo(nomeCampo, textoLongo);
						}else if(inteiro!=null) {
							linha.setCampo(nomeCampo, inteiro);
						}
					}
					
					linha.setCampo("VALIDADOR", "S");
				}
			}
			 
			
		} catch (Exception e) {
			System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO CARREGAR OS DADOS DE CONTRATOS "+e.getMessage());
			e.getStackTrace();
		}
	}
		
	//CADASTRO 1 --------------------------------------------------------------------
	
	//1.2
	private void cadastroUm(BigDecimal idflow, Registro linhas) {
		
		String tarefaCadastro = "UserTask_18cdx06";
		
		List<String> camposCadastro = new ArrayList<String>();
		camposCadastro.add("CD_CONTRATO");
		camposCadastro.add("CD_MOTIVOATRASO");
		camposCadastro.add("CD_OBSERVACAO");
		camposCadastro.add("CD_PARCEIRO");
		camposCadastro.add("CD_PARCDEMONSTRACAO");
		
		for(String campo:camposCadastro) {
			carregaDadosCadastroUm(idflow,campo,linhas,tarefaCadastro);
		}
		
		String responsavel = "CD_USURESP";
		String criacao = "CD_DHCRIACAO";
		String aceite = "CD_DHACEITE";
		String conclusao = "CD_DHCONCLUSAO";
		String setor = "cadastro um";		
		carregaDados(idflow,tarefaCadastro,linhas,responsavel,criacao,aceite,conclusao,setor);

	}
	
	//1.2.1
	private void carregaDadosCadastroUm(BigDecimal idFlow, String nomeCampo,Registro linha,String tarefaCadastro) {
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("InstanciaVariavel", "this.IDINSTPRN = ? AND this.NOME=? ", new Object[] { idFlow,nomeCampo }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

				if(DynamicVO!=null) {

					String texto = DynamicVO.asString("TEXTO");
					Timestamp data = DynamicVO.asTimestamp("DTA");
					BigDecimal valor = DynamicVO.asBigDecimal("NUMDEC");
					String textoLongo = DynamicVO.asString("TEXTOLONGO");
					BigDecimal inteiro = DynamicVO.asBigDecimal("NUMINT");
					
					if("CD_CONTRATO".equals(nomeCampo)) {
						linha.setCampo(nomeCampo, new BigDecimal(texto));
					}else if("CD_PARCEIRO".equals(nomeCampo)){
						linha.setCampo(nomeCampo, new BigDecimal(texto));
					}else if("CD_PARCDEMONSTRACAO".equals(nomeCampo)){
						linha.setCampo(nomeCampo, new BigDecimal(texto));
					}else {
						if(texto!=null) {
							linha.setCampo(nomeCampo, texto);
						}else if(data!=null) {
							linha.setCampo(nomeCampo, data);
						}else if(valor!=null) {
							linha.setCampo(nomeCampo, valor);
						}else if (textoLongo!=null) {
							linha.setCampo(nomeCampo, textoLongo);
						}else if(inteiro!=null) {
							linha.setCampo(nomeCampo, inteiro);
						}
					}
				}
			}
			
		} catch (Exception e) {
			System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO CARREGAR RESPONSAVEL TAREFA CADASTRO UM "+e.getMessage());
			e.getStackTrace();
		}
		
	}
	
	//CADASTRO NESPRESSO -------------------------------------------------------------
	
	//1.3
	private void cadastroNespresso(BigDecimal idflow, Registro linhas) {
		
		String tarefaCadastro = "UserTask_0j8jgtz";
		
		List<String> camposCadastro = new ArrayList<String>();
		camposCadastro.add("CD_PARCNESP");
		camposCadastro.add("NS_MOTIVOATRASO");
		camposCadastro.add("NS_OBSERVACAO");
		
		for(String campo:camposCadastro) {
			carregaDadosCadastroNespresso(idflow,campo,linhas,tarefaCadastro);
		}
		
		String responsavel = "NS_RESPONSAVEL";
		String criacao = "NS_DHCRIACAO";
		String aceite = "NS_DHACEITE";
		String conclusao = "NS_DHCONCLUSAO";
		String setor = "cadastro nespresso";		
		carregaDados(idflow,tarefaCadastro,linhas,responsavel,criacao,aceite,conclusao,setor);
		
	}
	
	//1.3.1
	private void carregaDadosCadastroNespresso(BigDecimal idFlow, String nomeCampo,Registro linha,String tarefaCadastro) {
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("InstanciaVariavel", "this.IDINSTPRN = ? AND this.NOME=? ", new Object[] { idFlow,nomeCampo }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

				if(DynamicVO!=null) {

					String texto = DynamicVO.asString("TEXTO");
					Timestamp data = DynamicVO.asTimestamp("DTA");
					BigDecimal valor = DynamicVO.asBigDecimal("NUMDEC");
					String textoLongo = DynamicVO.asString("TEXTOLONGO");
					BigDecimal inteiro = DynamicVO.asBigDecimal("NUMINT");
					
					if("CD_PARCNESP".equals(nomeCampo)) {
						linha.setCampo("NS_PARCNESP", new BigDecimal(texto));
					}else {
						if(texto!=null) {
							linha.setCampo(nomeCampo, texto);
						}else if(data!=null) {
							linha.setCampo(nomeCampo, data);
						}else if(valor!=null) {
							linha.setCampo(nomeCampo, valor);
						}else if (textoLongo!=null) {
							linha.setCampo(nomeCampo, textoLongo);
						}else if(inteiro!=null) {
							linha.setCampo(nomeCampo, inteiro);
						}
					}
				}
			}
			
		} catch (Exception e) {
			System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO CARREGAR RESPONSAVEL TAREFA CADASTRO NESPRESSO "+e.getMessage());
			e.getStackTrace();
		}
	}
	
	
	//PATRIMONIOS ---------------------------------------------------------------------
	
	//1.4
	private void Patrimonios(BigDecimal idflow,Registro linhas) {
		
		String tarefaCadastro = "UserTask_0qmvbmu";
		
		List<String> camposPatrimonio = new ArrayList<String>();
		camposPatrimonio.add("PT_MOTIVOATRASO");
		camposPatrimonio.add("PT_OBSERVACAO");
		
		for(String campo:camposPatrimonio) {
			carregaDadosPatrimonio(idflow,campo,linhas,tarefaCadastro);
		}
		
		String responsavel = "PT_RESPONSAVEL";
		String criacao = "PT_DHCRIACAO";
		String aceite = "PT_DHACEITE";
		String conclusao = "PT_DHCONCLUSAO";
		String setor = "patrimonios";		
		carregaDados(idflow,tarefaCadastro,linhas,responsavel,criacao,aceite,conclusao,setor);
		
		carregaPatrimoniosTabelaFlow(idflow);
	}
	
	//1.4.1
	private void carregaDadosPatrimonio(BigDecimal idFlow, String nomeCampo,Registro linha,String tarefaCadastro) {
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("InstanciaVariavel", "this.IDINSTPRN = ? AND this.NOME=? ", new Object[] { idFlow,nomeCampo }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				
				if(DynamicVO!=null) {

					String texto = DynamicVO.asString("TEXTO");
					Timestamp data = DynamicVO.asTimestamp("DTA");
					BigDecimal valor = DynamicVO.asBigDecimal("NUMDEC");
					String textoLongo = DynamicVO.asString("TEXTOLONGO");
					BigDecimal inteiro = DynamicVO.asBigDecimal("NUMINT");
					
					if(texto!=null) {
						linha.setCampo(nomeCampo, texto);
					}else if(data!=null) {
						linha.setCampo(nomeCampo, data);
					}else if(valor!=null) {
						linha.setCampo(nomeCampo, valor);
					}else if (textoLongo!=null) {
						linha.setCampo(nomeCampo, textoLongo);
					}else if(inteiro!=null) {
						linha.setCampo(nomeCampo, inteiro);
					}
				}
			}
			
		} catch (Exception e) {
			System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO CARREGAR RESPONSAVEL TAREFA CADASTRO PATRIMONIO "+e.getMessage());
			e.getStackTrace();
		}
	}
	
	//1.4.1
	private void carregaPatrimoniosTabelaFlow(BigDecimal idflow) {
		
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("AD_MAQUINASFLOW", "this.IDINSTPRN = ? ", new Object[] { idflow }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				
				if(DynamicVO!=null) {
					deletaPatrimoniosExistentes(idflow, DynamicVO.asString("CODBEM"));
					salvaPatrimonios(DynamicVO,idflow);
				}	
			}
		} catch (Exception e) {
			System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO CARREGAR AS INFO. DO PATRIMONIO"+e.getMessage());
			e.getStackTrace();
		}
	}
	
	//1.4.1.1
		private void deletaPatrimoniosExistentes(BigDecimal idflow, String codbem) {
			try {
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				dwfFacade.removeByCriteria(new FinderWrapper("AD_GERENCIAINSTPAT","this.IDFLOW=? AND this.CODBEM=? ", new Object[] { idflow,codbem }));
			} catch (Exception e) {
				System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO EXCLUIR PEDIDOS"+e.getMessage());
				e.getStackTrace();
			}			
		}
	
	//1.4.1.2
	private void salvaPatrimonios(DynamicVO patrimonios,BigDecimal idflow) {
		try {
			
			String novaSerie = patrimonios.asString("NOVASERIE");
			String serieAlterada = patrimonios.asString("SERIEALTERADA");
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_GERENCIAINSTPAT");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODBEM", patrimonios.asString("CODBEM"));
			VO.setProperty("DTPRODUCAO", patrimonios.asTimestamp("DTPRODUCAO"));
			VO.setProperty("IDFLOW", idflow);
			if(novaSerie!=null) {
				VO.setProperty("NOVASERIE", novaSerie);
			}
			VO.setProperty("RESPROD", patrimonios.asString("RESPROD"));
			if(serieAlterada!=null) {
				VO.setProperty("SERIEALTERADA", serieAlterada);
			}
			VO.setProperty("PRODUCAO", patrimonios.asString("PRODUCAO"));
			
			dwfFacade.createEntity("AD_GERENCIAINSTPAT", (EntityVO) VO);
			
		} catch (Exception e) {
			System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO SALVAR AS INFO. DO PATRIMONIO"+e.getMessage());
			e.getStackTrace();
		}
	}
	
	//PRODUCAO ---------------------------------------------------------------------
	
	//1.5
	private void producao(BigDecimal idflow, Registro linhas) {
		
		String tarefaProducao = "UserTask_0yj8z1s";
		
		List<String> camposCadastro = new ArrayList<String>();
		camposCadastro.add("PROD_MOTIVOATRASO");
		camposCadastro.add("PROD_OBSERVACAO");
		
		for(String campo:camposCadastro) {
			carregaDadosProducao(idflow,campo,linhas,tarefaProducao);
		}
		
		String responsavel = "PROD_RESPONSAVEL";
		String criacao = "PROD_DHCRIACAO";
		String aceite = "PROD_DHACEITE";
		String conclusao = "PROD_DHCONCLUSAO";
		String setor = "producao";		
		carregaDados(idflow,tarefaProducao,linhas,responsavel,criacao,aceite,conclusao,setor);
	}
	
	//1.5.1
	private void carregaDadosProducao(BigDecimal idFlow, String nomeCampo,Registro linha,String tarefaProducao) {
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("InstanciaVariavel", "this.IDINSTPRN = ? AND this.NOME=? ", new Object[] { idFlow,nomeCampo }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				
				if(DynamicVO!=null) {

					String texto = DynamicVO.asString("TEXTO");
					Timestamp data = DynamicVO.asTimestamp("DTA");
					BigDecimal valor = DynamicVO.asBigDecimal("NUMDEC");
					String textoLongo = DynamicVO.asString("TEXTOLONGO");
					BigDecimal inteiro = DynamicVO.asBigDecimal("NUMINT");
					
					if(texto!=null) {
						linha.setCampo(nomeCampo, texto);
					}else if(data!=null) {
						linha.setCampo(nomeCampo, data);
					}else if(valor!=null) {
						linha.setCampo(nomeCampo, valor);
					}else if (textoLongo!=null) {
						linha.setCampo(nomeCampo, textoLongo);
					}else if(inteiro!=null) {
						linha.setCampo(nomeCampo, inteiro);
					}
				}
			}
			
		} catch (Exception e) {
			System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO CARREGAR RESPONSAVEL TAREFA CADASTRO PRODUCAO "+e.getMessage());
			e.getStackTrace();
		}
	}
		
	//VENDAS ---------------------------------------------------------------------
	
	//1.6
	private void vendas(BigDecimal idflow, Registro linhas) {
		
		String tarefaCadastro = "UserTask_0k0pho2";
		
		List<String> camposPatrimonio = new ArrayList<String>();
		camposPatrimonio.add("VD_MOTIVOATRASO");
		camposPatrimonio.add("VD_OBSERVACAO");
		
		for(String campo:camposPatrimonio) {
			carregaDadosPedidos(idflow,campo,linhas,tarefaCadastro);
		}
		
		String responsavel = "VD_RESPONSAVEL";
		String criacao = "VD_DHCRIACAO";
		String aceite = "VD_DHACEITE";
		String conclusao = "VD_DHCONCLUSAO";
		String setor = "vendas";		
		carregaDados(idflow,tarefaCadastro,linhas,responsavel,criacao,aceite,conclusao,setor);
		carregaPedidosTabelaFlow(idflow);
	}
	
	//1.6.1
	private void carregaDadosPedidos(BigDecimal idFlow, String nomeCampo,Registro linha,String tarefaCadastro) {
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("InstanciaVariavel", "this.IDINSTPRN = ? AND this.NOME=? ", new Object[] { idFlow,nomeCampo }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				
				if(DynamicVO!=null) {

					String texto = DynamicVO.asString("TEXTO");
					Timestamp data = DynamicVO.asTimestamp("DTA");
					BigDecimal valor = DynamicVO.asBigDecimal("NUMDEC");
					String textoLongo = DynamicVO.asString("TEXTOLONGO");
					BigDecimal inteiro = DynamicVO.asBigDecimal("NUMINT");
					
					if(texto!=null) {
						linha.setCampo(nomeCampo, texto);
					}else if(data!=null) {
						linha.setCampo(nomeCampo, data);
					}else if(valor!=null) {
						linha.setCampo(nomeCampo, valor);
					}else if (textoLongo!=null) {
						linha.setCampo(nomeCampo, textoLongo);
					}else if(inteiro!=null) {
						linha.setCampo(nomeCampo, inteiro);
					}
				}
			}
			
		} catch (Exception e) {
			System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO CARREGAR RESPONSAVEL TAREFA VENDAS "+e.getMessage());
			e.getStackTrace();
		}
	}
	
	//1.6.2
	private void carregaPedidosTabelaFlow(BigDecimal idflow) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("AD_PEDIDOSRELINST", "this.IDINSTPRN = ? ", new Object[] { idflow }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				
				if(DynamicVO!=null) {
					deletaPedidosExistentes(idflow,DynamicVO.asBigDecimal("NUNOTA"));
					salvaPedidos(DynamicVO,idflow);
				}	
			}
		} catch (Exception e) {
			System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO CARREGAR AS INFO. DO PEDIDO DE VENDA"+e.getMessage());
			e.getStackTrace();
		}
	}
	
	//1.6.2.1
	private void deletaPedidosExistentes(BigDecimal idflow, BigDecimal nunota) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_GERENCIAINSTPEDIDO","this.IDFLOW=? AND this.NUNOTA=? ", new Object[] { idflow,nunota }));

		} catch (Exception e) {
			System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO EXCLUIR PEDIDOS"+e.getMessage());
			e.getStackTrace();
		}			
	}
	
	//1.6.2.2
	private void salvaPedidos(DynamicVO pedido,BigDecimal idflow) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_GERENCIAINSTPEDIDO");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("NUNOTA", pedido.asBigDecimal("NUNOTA"));
			VO.setProperty("IDFLOW", idflow);
			VO.setProperty("FATURADO", pedido.asString("FATURADO"));
			VO.setProperty("OBSERVACAO", pedido.asString("OBSERVACAO"));
			
			dwfFacade.createEntity("AD_GERENCIAINSTPEDIDO", (EntityVO) VO);
			
		} catch (Exception e) {
			System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO SALVAR AS INFO. DO PEDIDO DE VENDAS"+e.getMessage());
			e.getStackTrace();
		}
	}
	
	//PLANEJAMENTO ---------------------------------------------------------------------
	
	//1.7
	private void planejamento(BigDecimal idflow, Registro linhas) {
		String tarefaProducao = "UserTask_102vtp7";
		
		List<String> camposCadastro = new ArrayList<String>();
		camposCadastro.add("PL_OSALTERADA");
		camposCadastro.add("PL_EMPRESANOTA");
		camposCadastro.add("PL_MOTIVOATRASO");
		camposCadastro.add("PL_OBSERVACAO");
		camposCadastro.add("SISTEMA_NROS");
		
		for(String campo:camposCadastro) {
			carregaDadosPlanejamento(idflow,campo,linhas,tarefaProducao);
		}
		
		String responsavel = "PL_RESPONSAVEL";
		String criacao = "PL_DHCRIACAO";
		String aceite = "PL_DHACEITE";
		String conclusao = "PL_DHCONCLUSAO";
		String setor = "planejamento";		
		carregaDados(idflow,tarefaProducao,linhas,responsavel,criacao,aceite,conclusao,setor);
	}
	
	//1.7.1
	private void carregaDadosPlanejamento(BigDecimal idFlow, String nomeCampo,Registro linha,String tarefaProducao) {
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("InstanciaVariavel", "this.IDINSTPRN = ? AND this.NOME=? ", new Object[] { idFlow,nomeCampo }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				
				if(DynamicVO!=null) {

					String texto = DynamicVO.asString("TEXTO");
					Timestamp data = DynamicVO.asTimestamp("DTA");
					BigDecimal valor = DynamicVO.asBigDecimal("NUMDEC");
					String textoLongo = DynamicVO.asString("TEXTOLONGO");
					BigDecimal inteiro = DynamicVO.asBigDecimal("NUMINT");
					
					if("PL_EMPRESANOTA".equals(nomeCampo)) {
						linha.setCampo(nomeCampo, new BigDecimal(texto));
					}else if("SISTEMA_NROS".equals(nomeCampo)){
						linha.setCampo("PL_NUMOS", new BigDecimal(texto));
					}else {
						if(texto!=null) {
							linha.setCampo(nomeCampo, texto);
						}else if(data!=null) {
							linha.setCampo(nomeCampo, data);
						}else if(valor!=null) {
							linha.setCampo(nomeCampo, valor);
						}else if (textoLongo!=null) {
							linha.setCampo(nomeCampo, textoLongo);
						}else if(inteiro!=null) {
							linha.setCampo(nomeCampo, inteiro);
						}
					}	
				}
			}
			
		} catch (Exception e) {
			System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO CARREGAR RESPONSAVEL TAREFA PLANEJAMENTO "+e.getMessage());
			e.getStackTrace();
		}
	}
	
	//FATURAMENTO REMESSA ---------------------------------------------------------------------
	
	//1.8
	private void FaturamentoRemessa(BigDecimal idflow, Registro linhas) {
		String tarefaProducao = "UserTask_1vbovxf";
		
		List<String> camposCadastro = new ArrayList<String>();
		camposCadastro.add("FT_NOTAREMESSA");
		camposCadastro.add("FT_NOTACONFIRMADA");
		camposCadastro.add("FT_MOTIVOATRASO");
		camposCadastro.add("FT_OBSERVACAO");
		
		for(String campo:camposCadastro) {
			carregaDadosFaturamentoRemessa(idflow,campo,linhas,tarefaProducao);
		}
		
		String responsavel = "FT_RESPONSAVEL";
		String criacao = "FT_DHCRIACAO";
		String aceite = "FT_DHACEITE";
		String conclusao = "FT_DHCONCLUSAO";
		String setor = "Faturamento Remessa";		
		carregaDados(idflow,tarefaProducao,linhas,responsavel,criacao,aceite,conclusao,setor);
	}
	
	//1.8.1
	private void carregaDadosFaturamentoRemessa(BigDecimal idFlow, String nomeCampo,Registro linha,String tarefaProducao) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("InstanciaVariavel", "this.IDINSTPRN = ? AND this.NOME=? ", new Object[] { idFlow,nomeCampo }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				
				if(DynamicVO!=null) {

					String texto = DynamicVO.asString("TEXTO");
					Timestamp data = DynamicVO.asTimestamp("DTA");
					BigDecimal valor = DynamicVO.asBigDecimal("NUMDEC");
					String textoLongo = DynamicVO.asString("TEXTOLONGO");
					BigDecimal inteiro = DynamicVO.asBigDecimal("NUMINT");
					
					if(texto!=null) {
						linha.setCampo(nomeCampo, texto);
					}else if(data!=null) {
						linha.setCampo(nomeCampo, data);
					}else if(valor!=null) {
						linha.setCampo(nomeCampo, valor);
					}else if (textoLongo!=null) {
						linha.setCampo(nomeCampo, textoLongo);
					}else if(inteiro!=null) {
						linha.setCampo(nomeCampo, inteiro);
					}
				}
			}
						
		} catch (Exception e) {
			System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO CARREGAR RESPONSAVEL TAREFA FAT. REMESSA "+e.getMessage());
			e.getStackTrace();
		}
	}
		
	//LOGISTICA --------------------------------------------------------------------------------
	
	//1.9
	private void logistica(BigDecimal idflow, Registro linhas) {
		String tarefaProducao = "UserTask_13zgx3o";
		
		List<String> camposCadastro = new ArrayList<String>();
		camposCadastro.add("LG_MOTIVOATRASO");
		camposCadastro.add("LG_OBSERVACAO");
		
		for(String campo:camposCadastro) {
			carregaDadoslogistica(idflow,campo,linhas,tarefaProducao);
		}
		
		String responsavel = "LG_RESPONSAVEL";
		String criacao = "LG_DHCRIACAO";
		String aceite = "LG_DHACEITE";
		String conclusao = "LG_DHCONCLUSAO";
		String setor = "Logistica";		
		carregaDados(idflow,tarefaProducao,linhas,responsavel,criacao,aceite,conclusao,setor);
	}
	
	//1.9.1
	private void carregaDadoslogistica(BigDecimal idFlow, String nomeCampo,Registro linha,String tarefaProducao) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("InstanciaVariavel", "this.IDINSTPRN = ? AND this.NOME=? ", new Object[] { idFlow,nomeCampo }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				
				if(DynamicVO!=null) {

					String texto = DynamicVO.asString("TEXTO");
					Timestamp data = DynamicVO.asTimestamp("DTA");
					BigDecimal valor = DynamicVO.asBigDecimal("NUMDEC");
					String textoLongo = DynamicVO.asString("TEXTOLONGO");
					BigDecimal inteiro = DynamicVO.asBigDecimal("NUMINT");
					
					if(texto!=null) {
						linha.setCampo(nomeCampo, texto);
					}else if(data!=null) {
						linha.setCampo(nomeCampo, data);
					}else if(valor!=null) {
						linha.setCampo(nomeCampo, valor);
					}else if (textoLongo!=null) {
						linha.setCampo(nomeCampo, textoLongo);
					}else if(inteiro!=null) {
						linha.setCampo(nomeCampo, inteiro);
					}
				}
			}
			
		} catch (Exception e) {
			System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO CARREGAR RESPONSAVEL TAREFA LOGISTICA "+e.getMessage());
			e.getStackTrace();
		}
	}
	
	//CADASTRO ATIV. LOCAÇÃO ---------------------------------------------------------------------
	
	//1.10
	private void AtivacaoLocacao(BigDecimal idflow, Registro linhas) {
		String tarefaProducao = "UserTask_0qffc76";
		
		List<String> camposCadastro = new ArrayList<String>();
		camposCadastro.add("AL_MOTIVOATRASO");
		camposCadastro.add("AL_OBSERVACAO");
		
		for(String campo:camposCadastro) {
			carregaDadosAtivacaoLocacao(idflow,campo,linhas,tarefaProducao);
		}
		
		String responsavel = "AL_RESPONSAVEL";
		String criacao = "AL_DHCRIACAO";
		String aceite = "AL_DHACEITE";
		String conclusao = "AL_DHCONCLUSAO";
		String setor = "Ativacao Locação";		
		carregaDados(idflow,tarefaProducao,linhas,responsavel,criacao,aceite,conclusao,setor);
	}

	//1.10.1
	private void carregaDadosAtivacaoLocacao(BigDecimal idFlow, String nomeCampo,Registro linha,String tarefaProducao) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("InstanciaVariavel", "this.IDINSTPRN = ? AND this.NOME=? ", new Object[] { idFlow,nomeCampo }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				
				if(DynamicVO!=null) {

					String texto = DynamicVO.asString("TEXTO");
					Timestamp data = DynamicVO.asTimestamp("DTA");
					BigDecimal valor = DynamicVO.asBigDecimal("NUMDEC");
					String textoLongo = DynamicVO.asString("TEXTOLONGO");
					BigDecimal inteiro = DynamicVO.asBigDecimal("NUMINT");
					
					if(texto!=null) {
						linha.setCampo(nomeCampo, texto);
					}else if(data!=null) {
						linha.setCampo(nomeCampo, data);
					}else if(valor!=null) {
						linha.setCampo(nomeCampo, valor);
					}else if (textoLongo!=null) {
						linha.setCampo(nomeCampo, textoLongo);
					}else if(inteiro!=null) {
						linha.setCampo(nomeCampo, inteiro);
					}
				}
			}
			
		} catch (Exception e) {
			System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO CARREGAR RESPONSAVEL TAREFA ATIV. LOCAÇÃO "+e.getMessage());
			e.getStackTrace();
		}
	}
	
	//FATURAMENTO CONTRATO ---------------------------------------------------------------------
	
	//1.11
	private void faturamentoContrato(BigDecimal idflow, Registro linhas) {
		String tarefaProducao = "UserTask_1dnrm0e";
		
		List<String> camposCadastro = new ArrayList<String>();
		camposCadastro.add("FC_MOTIVOATRASO");
		camposCadastro.add("FC_OBSERVACAO");
		camposCadastro.add("FC_NUNOTA");
		camposCadastro.add("FC_FATURADO");
		
		for(String campo:camposCadastro) {
			carregaDadosFaturamentoContrato(idflow,campo,linhas,tarefaProducao);
		}
		
		String responsavel = "FC_RESPONSAVEL";
		String criacao = "FC_DHCRIACAO";
		String aceite = "FC_DHACEITE";
		String conclusao = "FC_DHCONCLUSAO";
		String setor = "Faturamento Contrato";		
		carregaDados(idflow,tarefaProducao,linhas,responsavel,criacao,aceite,conclusao,setor);
	}
	
	//1.11.1
	private void carregaDadosFaturamentoContrato(BigDecimal idFlow, String nomeCampo,Registro linha,String tarefaProducao) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("InstanciaVariavel", "this.IDINSTPRN = ? AND this.NOME=? ", new Object[] { idFlow,nomeCampo }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				
				if(DynamicVO!=null) {

					String texto = DynamicVO.asString("TEXTO");
					Timestamp data = DynamicVO.asTimestamp("DTA");
					BigDecimal valor = DynamicVO.asBigDecimal("NUMDEC");
					String textoLongo = DynamicVO.asString("TEXTOLONGO");
					BigDecimal inteiro = DynamicVO.asBigDecimal("NUMINT");

					if(texto!=null) {
						linha.setCampo(nomeCampo, texto);
					}else if(data!=null) {
						linha.setCampo(nomeCampo, data);
					}else if(valor!=null) {
						linha.setCampo(nomeCampo, valor);
					}else if (textoLongo!=null) {
						linha.setCampo(nomeCampo, textoLongo);
					}else if(inteiro!=null) {
						linha.setCampo(nomeCampo, inteiro);
					}
				}
			}
						
		} catch (Exception e) {
			System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO CARREGAR RESPONSAVEL TAREFA FAT. CONTRATO "+e.getMessage());
			e.getStackTrace();
		}
	}

	
	//DADOS DO SOLICITANTE ---------------------------------------------------------------------
	
	//2.0
	//@SuppressWarnings("deprecation")
	private void buscaDadosSolicitante(BigDecimal idflow, Registro linhas) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("AD_EMAILSFLOW", "this.IDINSTPRN = ? ", new Object[] { idflow }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);

				if (DynamicVO != null) {
										
					linhas.setCampo("SIS_IDEMAIL", DynamicVO.asBigDecimal("EMAIL_ID"));
					linhas.setCampo("SIS_ASSUNTO", DynamicVO.asString("EMAIL_ASSUNTO"));
					linhas.setCampo("SIS_CONTEUDO", DynamicVO.asString("EMAIL_CONTEUDO"));
					linhas.setCampo("SIS_SOLICITANTE", DynamicVO.asString("EMAIL_SOLICITANTE"));
					
					idEmail = DynamicVO.asBigDecimal("EMAIL_ID");
				}

			}
			
		} catch (Exception e) {
			System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO CARREGAR DADOS DO RESPONSAVEL PELA SOLICITAÇÃO DO FLOW"+e.getMessage());
			e.getStackTrace();
		}
	}
	
	//CARREGAR ANEXO COMERCIAL ---------------------------------------------------------------------
	
	//3.0
	private void verificaAnexos(BigDecimal idflow, Registro linhas) {
		deletarAnexos(idflow);
		buscaAnexoComercial(idflow,linhas);
	}
	
	//3.1
	private void deletarAnexos(BigDecimal idflow) {
		
		String instancia="AD_GERENCIAINST";
		String pk = idflow+"_AD_GERENCIAINST";
		
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AnexoSistema","this.PKREGISTRO=? and this.NOMEINSTANCIA=?", new Object[] { pk,instancia }));
		} catch (Exception e) {
			System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO DELETAR OS ANEXOS"+e.getMessage());
			e.getStackTrace();
		}
	}
	
	//3.2
	private void buscaAnexoComercial(BigDecimal idflow, Registro linhas) {
		
		try {
			
			if(idEmail!=null) {
				
				EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
				Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_ANEXOSEMAILFLOW","this.ID = ? ", new Object[] { idEmail }));
				for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

					if (DynamicVO != null) {
						
						String repoArquivos = DynamicVO.asString("REPOANEXO");
						String nome = DynamicVO.asString("NOME");
						String pkRegistro = idflow+"_AD_GERENCIAINST";

						if(repoArquivos!=null) {
							EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
							EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AnexoSistema");
							DynamicVO VO = (DynamicVO) NPVO;
							
							VO.setProperty("NUATTACH", pegaUltimoIdAnexos());
							VO.setProperty("NOMEINSTANCIA", "AD_GERENCIAINST");
							VO.setProperty("CHAVEARQUIVO", repoArquivos);
							VO.setProperty("NOMEARQUIVO", nome);
							VO.setProperty("DESCRICAO", "Anexo Comercial");
							VO.setProperty("RESOURCEID", "br.com.sankhya.menu.adicional.AD_GERENCIAINST");
							VO.setProperty("TIPOAPRES", "LOC");
							VO.setProperty("TIPOACESSO", "ALL");
							VO.setProperty("CODUSU", new BigDecimal(0));
							VO.setProperty("DHALTER", new Timestamp(System.currentTimeMillis()));
							VO.setProperty("PKREGISTRO", pkRegistro);
							VO.setProperty("CODUSUALT", new BigDecimal(0));
							VO.setProperty("DHCAD", new Timestamp(System.currentTimeMillis()));
							
							dwfFacade.createEntity("AnexoSistema", (EntityVO) VO);
						}
					}
				}
			}
	
		} catch (Exception e) {
			System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO CARREGAR ANEXO"+e.getMessage());
			e.getStackTrace();
		}	
	}
	
	
	//MÉTODO COMPLEMENTAR - PEGA ULTIMO ID DOS ANEXOS
	private BigDecimal pegaUltimoIdAnexos() throws Exception {
		
		int count = 0;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT MAX(NUATTACH)+1 AS CODFILA FROM TSIANX");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getInt("CODFILA");
		}
		
		BigDecimal ultimoCodigo = new BigDecimal(count);
		
		return ultimoCodigo;
				
	}
	
	
	//MÉTODO AUXILIAR - CARREGA RESPONSAVEL
		private void carregaDados(BigDecimal idflow, String tarefa, Registro linha, String responsavel, 
				String criacao, String aceite, String conclusao, String setor) {
			try {
				
				EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

				Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("InstanciaTarefa",
						"this.IDINSTPRN = ? AND this.IDELEMENTO=?", new Object[] { idflow, tarefa }));

				for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

					PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
					DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

					if (DynamicVO != null) {
						
						BigDecimal dono = DynamicVO.asBigDecimal("CODUSUDONO");
						if(dono!=null) {
							linha.setCampo(responsavel, dono);
						}
						
						Timestamp dtCriacao = DynamicVO.asTimestamp("DHCRIACAO");
						if(dtCriacao!=null) {
							linha.setCampo(criacao, dtCriacao);
							
						}
						
						if(setor.equals("contratos")) {
							linha.setCampo("SIS_DATA", dtCriacao);
						}
						
						
						Timestamp dtAceite = DynamicVO.asTimestamp("DHACEITE");
						if(dtAceite!=null) {
							linha.setCampo(aceite, dtAceite);
						}
						
						Timestamp dtConclusao = DynamicVO.asTimestamp("DHCONCLUSAO");
						if(dtConclusao!=null) {
							linha.setCampo(conclusao, dtConclusao);
							
							if(setor.equals("Faturamento Contrato")) {
								linha.setCampo("FINALIZADO", "S");
							}
						}	
					}
				}	
			} catch (Exception e) {
				System.out.println("## BTN FLOW CARREGA DADOS ## - ERRO AO CARREGAR RESPONSAVEL TAREFA: "+setor+" "+e.getMessage());
				e.getStackTrace();
			}
		}
}
